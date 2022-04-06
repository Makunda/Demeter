/*
 * Copyright (C) 2020  Hugo JOBY
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License v3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public v3
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.demeter.services.backup;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import javax.swing.text.html.Option;
import java.util.*;

public class BackupService {


    /**
     * Get group of nodes per taxonomy
     *
     * @param neo4jAL      Neo4j Access Layer
     * @param application  Name of the application
     * @param propertyName Name of the property
     * @return A mapping between taxonomy and the nodes
     */
    public static Map<String, List<Long>> getCompareTaxonomy(
            Neo4jAL neo4jAL, String application, String propertyName) throws Exception {
        // Request : Verify the differences between the save property and th actual state in the
        // application
        String request =
                String.format(
                        "MATCH (l:Level5:`%1$s`)-[:Aggregates]->(o:Object:`%1$s`) WHERE EXISTS(o.`%2$s`) AND l.FullName<>o.`%2$s` "
                                + "RETURN DISTINCT o.`%2$s` as taxonomy, ID(o) as idNode;",
                        application, propertyName);
        Map<String, List<Long>> returnMap = new HashMap<>();

        try {
            Map<String, Object> record;
            String taxonomy;
            Long id;

            // Execute the query
            Result res = neo4jAL.executeQuery(request);
            while (res.hasNext()) {
                record = res.next();
                taxonomy = (String) record.get("taxonomy");
                id = (long) record.get("idNode");

                // Verify Taxonomy
                if (taxonomy.split("##").length != 5) {
                    neo4jAL.logError(
                            String.format(
                                    "Incorrect taxonomy '%s' detected on node with id [%d].", taxonomy, id));
                    continue; // Skip the node
                }

                // Insert into the hashmap of results
                returnMap.putIfAbsent(taxonomy, new ArrayList<>());
                returnMap.get(taxonomy).add(id);
            }

            return returnMap;
        } catch (Neo4jQueryException e) {
            neo4jAL.logError("Failed to get the differences within the taxonomy.", e);
            throw new Exception("Failed to get differences in the application taxonomy.");
        }
    }

    /**
     * Get The Taxonomy of level 5 in the application
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application to investigate
     * @return A mapping between the id of the level and its corresponding taxonomy
     * @throws Neo4jQueryException
     */
    public static Map<Long, String> getLevel5Taxonomy(Neo4jAL neo4jAL, String application)
            throws Neo4jQueryException {
        return getTaxonomyProperty(neo4jAL, application, "FullName");
    }

    /**
     * Get a taxonomy style property of the level in the application, and the ID of the level 5
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     */
    public static Map<Long, String> getTaxonomyProperty(
            Neo4jAL neo4jAL, String application, String propertyName) throws Neo4jQueryException {
        String req =
                String.format(
                        "MATCH (o:Level5:`%s`) RETURN DISTINCT ID(o) as idLevel, o.`%s` as taxonomy",
                        application, propertyName);
        Map<Long, String> levelMap = new HashMap<>();
        Result res;

        // Extract the Level 5 taxonomy
        try {
            res = neo4jAL.executeQuery(req);
        } catch (Neo4jQueryException e) {
            neo4jAL.logError("Failed to extract the list of the level 5", e);
            throw e;
        }

        // Parse the results
        Long idNode;
        String property;
        while (res.hasNext()) {
            Map<String, Object> record = res.next();
            idNode = (Long) record.get("idLevel");
            property = (String) record.get("taxonomy");

            // Skip if fullName is null
            if (property == null) {
                neo4jAL.logError(
                        String.format(
                                "Failed to extract the taxonomy for level '%s' (id: [%s] ). Property '%s' cannot be null.",
                                propertyName, idNode, propertyName));
                continue;
            }

            // Filter poorly formatted taxonomy
            if (property.split("##").length != 5) {
                neo4jAL.logError(
                        String.format(
                                "Failed to extract the taxonomy for level '%s' (id: [%s] ). Incorrect taxonomy.",
                                propertyName, idNode));
                continue;
            }

            // Add to the map
            levelMap.put(idNode, property);
        }

        // If no results
        if (levelMap.isEmpty()) {
            neo4jAL.logInfo("No level were found during the investigation. " +
                    "Please check the name of the application and try again.");
        }

        return levelMap;
    }

}
