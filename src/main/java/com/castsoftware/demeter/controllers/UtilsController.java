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

import com.castsoftware.exporter.io.Exporter;
import com.castsoftware.exporter.io.Importer;
import com.castsoftware.exporter.results.OutputMessage;
import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.*;
import org.neo4j.graphdb.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class UtilsController {

    private static final String ERROR_PREFIX = "UTICx";
    private static final List<String> ALL_LABELS = Arrays.asList( ConfigurationNode.getLabel(), UseCaseNode.getLabel(), TagNode.getLabel(), StatisticNode.getLabel(), DocumentNode.getLabel());
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");
    private static final String USE_CASE_TO_TAG_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_tag");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    /**
     * Delete all the nodes related to the configuration
     * @param neo4jAL Neo4J Access layer
     * @return total number of node deleted
     * @throws Neo4jQueryException If an error is thrown during the process
     */
    public static int deleteTaggingNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
        // Retrieve every node label
        int numDeleted = 0;

        for(String labelAsString : ALL_LABELS) {
            numDeleted += neo4jAL.deleteAllNodesByLabel(Label.label(labelAsString));
        }

        return numDeleted;
    }

    /**
     * Save All nodes related to the configuration, in the specific directory
     * @param neo4jAL Neo4J Access layer
     * @param path Path where the file will be created
     * @param filename Name of the file
     * @throws ProcedureException
     * @return
     */
    public static Stream<OutputMessage> exportConfiguration(Neo4jAL neo4jAL, String path, String filename) throws com.castsoftware.exporter.exceptions.ProcedureException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String forgedFilename = filename + "_" + sdf.format(timestamp);

        Exporter exporter = new Exporter(neo4jAL.getDb(), neo4jAL.getLogger());
        return exporter.save(ALL_LABELS, path, forgedFilename, true, false);
    }

    /**
     * Load a previously saved configuration. Can load any "hfexporter" formatted zip file.
     * @param neo4jAL Neo4J Access layer
     * @param path Path where the configuration is saved
     * @throws ProcedureException
     * @return
     */
    public static Stream<OutputMessage> importConfiguration(Neo4jAL neo4jAL, String path) throws com.castsoftware.exporter.exceptions.ProcedureException {
        Importer importer = new Importer(neo4jAL.getDb(), neo4jAL.getLogger());
        return importer.load(path);
    }



    /**
     * Check all TagRequest present in the database. And return a report as a <code>String</code> indicating the percentage of working Queries.
     * @param neo4jAL Neo4j Access Layer
     * @param applicationContext The application to use as a context for the query
     * @return <code>String</code> The number of working / not working nodes and the percentage of success.
     * @throws Neo4jQueryException
     */
    public static String checkTags(Neo4jAL neo4jAL, String applicationContext) throws Neo4jQueryException {
        int valid = 0;
        int notValid = 0;

        Label tagLabel = Label.label(TagNode.getLabel());

        for (ResourceIterator<Node> it = neo4jAL.findNodes(tagLabel); it.hasNext(); ) {
            Node n = it.next();

            try {
                TagNode tn = TagNode.fromNode(neo4jAL, n);
                if(tn.checkQuery(applicationContext)) valid++;
                else notValid++;

            } catch (Exception | Neo4jBadNodeFormatException | Neo4jBadRequestException | Neo4jNoResult e) {
                neo4jAL.getLogger().error(String.format("An error occurred while retrieving TagNode with id \"%d\" was ignored.", n.getId()), e);
            }

        }

        double total = (double) (valid + notValid);
        double p = (double) (valid ) / total ;
        return String.format("%s TagRequest nodes were checked. %d valid node(s) were discovered. %d nonfunctional node(s) were identified. Percentage of success : %.2f", total, valid, notValid, p);
    }

}
