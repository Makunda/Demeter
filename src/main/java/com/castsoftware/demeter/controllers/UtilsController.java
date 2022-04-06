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

package com.castsoftware.demeter.controllers;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.BackupNode;
import com.castsoftware.demeter.models.demeter.*;
import com.castsoftware.demeter.utils.Workspace;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class UtilsController {

    private static final String ERROR_PREFIX = "UTICx";
    private static final List<String> ALL_LABELS =
            Arrays.asList(
                    ConfigurationNode.getLabel(),
                    UseCaseNode.getLabel(),
                    TagNode.getLabel(),
                    StatisticNode.getLabel(),
                    DocumentNode.getLabel(),
                    BackupNode.getLabel());
    private static final String USE_CASE_RELATIONSHIP =
            Configuration.get("neo4j.relationships.use_case.to_use_case");
    private static final String USE_CASE_TO_TAG_RELATIONSHIP =
            Configuration.get("neo4j.relationships.use_case.to_tag");

    private static final String OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String OBJECT_TAG_PROPERTY =
            Configuration.get("imaging.link.object_property.tags");
    private static final String TAG_PREFIX = Configuration.get("demeter.prefix.tags");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    /**
     * Delete all the nodes related to the configuration
     *
     * @param neo4jAL Neo4J Access layer
     * @return total number of node deleted
     * @throws Neo4jQueryException If an error is thrown during the process
     */
    public static int deleteTaggingNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
        // Retrieve every node label
        int numDeleted = 0;

        for (String labelAsString : ALL_LABELS) {
            numDeleted += neo4jAL.deleteAllNodesByLabel(Label.label(labelAsString));
        }

        return numDeleted;
    }

    /**
     * Remove all the tags applied by the procedure
     *
     * @param neo4jAL Neo4J Access layer
     * @return total number of node concerned by the removed
     * @throws Neo4jQueryException If an error is thrown during the process
     */
    public static int removeTags(Neo4jAL neo4jAL) throws Neo4jQueryException {
        // Retrieve every node label
        int numDeleted = 0;

        String forgedRemoveTags =
                String.format(
                        "MATCH (n:%1$s) WHERE EXISTS(n.%2$s) SET n.%2$s = [x IN n.%2$s WHERE NOT x CONTAINS '%3$s'] RETURN COUNT(n) as del",
                        OBJECT_LABEL, OBJECT_TAG_PROPERTY, TAG_PREFIX);

        Result res = neo4jAL.executeQuery(forgedRemoveTags);
        if (res.hasNext()) {
            Long deleted = (Long) res.next().get("del");
            numDeleted = deleted.intValue();
        }

        return numDeleted;
    }

    /**
     * Remove all the tags applied by the procedure
     *
     * @param workspace The new Demeter Workspace
     * @return total number of node deleted
     * @throws Neo4jQueryException If an error is thrown during the process
     */
    public static List<String> setWorkspace(String workspace) throws FileNotFoundException {

        Path newDirectory = Path.of(workspace);
        if (!Files.exists(newDirectory)) {
            return List.of("The directory specified doesn't exist. Make sure the directory exists.");
        }
        Configuration.set("demeter.workspace.path", newDirectory.toAbsolutePath().toString());
        // Reload the configuration
        Configuration.saveAndReload();

        List<String> returnList = new ArrayList<>(Workspace.validateWorkspace());

        return returnList;
    }

    /**
     * Get the actual value of the Workspace
     *
     * @return
     * @throws FileNotFoundException
     */
    public static String getWorkspace() throws FileNotFoundException {
        return Configuration.get("demeter.workspace.path");
    }

    /**
     * Check all TagRequest present in the database. And return a report as a <code>String</code>
     * indicating the percentage of working Queries.
     *
     * @param neo4jAL            Neo4j Access Layer
     * @param applicationContext The application to use as a context for the query
     * @return <code>String</code> The number of working / not working nodes and the percentage of
     * success.
     * @throws Neo4jQueryException
     */
    public static String checkTags(Neo4jAL neo4jAL, String applicationContext)
            throws Neo4jQueryException {
        int valid = 0;
        int notValid = 0;

        Label tagLabel = Label.label(TagNode.getLabel());

        for (ResourceIterator<Node> it = neo4jAL.findNodes(tagLabel); it.hasNext(); ) {
            Node n = it.next();

            try {
                TagNode tn = TagNode.fromNode(neo4jAL, n);
                if (tn.checkQuery(applicationContext)) valid++;
                else notValid++;

            } catch (Exception
                    | Neo4jBadNodeFormatException
                    | Neo4jBadRequestException
                    | Neo4jNoResult e) {
                neo4jAL
                        .getLogger()
                        .error(
                                String.format(
                                        "An error occurred while retrieving TagNode with id \"%d\" was ignored.",
                                        n.getId()),
                                e);
            }
        }

        double total = valid + notValid;
        double p = (double) (valid) / total;
        return String.format(
                "%s TagRequest nodes were checked. %d valid node(s) were discovered. %d nonfunctional node(s) were identified. Percentage of success : %.2f",
                total, valid, notValid, p);
    }
}