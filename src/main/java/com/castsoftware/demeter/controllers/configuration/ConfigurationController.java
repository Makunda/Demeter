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

package com.castsoftware.demeter.controllers.configuration;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.demeter.ConfigurationNode;
import com.castsoftware.demeter.models.demeter.DocumentNode;
import com.castsoftware.demeter.models.demeter.TagNode;
import com.castsoftware.demeter.models.demeter.UseCaseNode;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.List;
import java.util.NoSuchElementException;

public class ConfigurationController {

    private static final String ERROR_PREFIX = "CONFCx";
    private static final String USE_CASE_RELATIONSHIP =
            Configuration.get("neo4j.relationships.use_case.to_use_case");
    private static final String TAG_RETURN_LABEL_VAL =
            Configuration.get("tag.anchors.return.return_val");

    /**
     * Create a new configuration node
     *
     * @param neo4jAL Neo4j Access Layer
     * @param name    Name of the new configuration
     * @return The configuration node created
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public static Node createConfiguration(Neo4jAL neo4jAL, String name)
            throws Neo4jBadRequestException, Neo4jNoResult {
        ConfigurationNode n = new ConfigurationNode(neo4jAL, name);
        return n.createNode();
    }

    /**
     * Delete a configuration and its associated nodes.
     *
     * @param neo4jAL Neo4J Access Layer
     * @param id      ID of the configuration node to delete
     * @return The number of nodes deleted
     * @throws Neo4jQueryException
     */
    public static Long deleteConfiguration(Neo4jAL neo4jAL, Long id) throws Neo4jQueryException {
        String initQuery =
                String.format(
                        "MATCH p=(n:%s)-[:%s*]->(:%s)-[:%s*]->(:%s) WHERE ID(n)=%d return COUNT(p) as deleted_node",
                        ConfigurationNode.getLabel(),
                        USE_CASE_RELATIONSHIP,
                        UseCaseNode.getLabel(),
                        USE_CASE_RELATIONSHIP,
                        TagNode.getLabel(),
                        id);
        try {
            Result res = neo4jAL.executeQuery(initQuery);
            return (Long) res.next().get("deleted_node");
        } catch (NoSuchElementException ex) {
            throw new Neo4jQueryException(
                    String.format("Cannot delete the configuration with ID : %d", id),
                    initQuery,
                    ex,
                    ERROR_PREFIX + "DELC1");
        }
    }

    /**
     * Execute the actual configuration
     *
     * @param neo4jAL           Neo4J Access layer
     * @param configurationName Name of the configuration to execute
     * @param applicationLabel  Label of the application on which the tag will be applied
     * @return Number of tag applied in the configuration
     * @throws ProcedureException
     */
    public static int executeConfiguration(
            Neo4jAL neo4jAL, String configurationName, String applicationLabel)
            throws Neo4jBadRequestException, Neo4jQueryException, Neo4jNoResult {
        List<Label> labels = neo4jAL.getAllLabels();

        // Verify if the label is present in the database
        if (!labels.contains(Label.label(applicationLabel))) {
            String message = String.format("Cannot find label \"%s\" in the database", applicationLabel);
            throw new Neo4jBadRequestException(message, ERROR_PREFIX + "EXEC1");
        }

        int nExecution = 0;

        // Execute activated tag's requests
        List<TagNode> tags = TagController.getSelectedTags(neo4jAL, configurationName);

        for (TagNode n : tags) {
            try {
                List<Node> res = n.executeRequest(applicationLabel); // Results need to be processed
                neo4jAL.logInfo("Statistics saved for tag : " + n.getTag());
                nExecution++;
            } catch (Exception | Neo4jNoResult | Neo4jBadRequestException err) {
                neo4jAL
                        .getLogger()
                        .error(
                                "An error occurred during Tag request execution. Tag with Node ID : "
                                        + n.getNodeId(),
                                err);
            }
        }

        // Execute DocumentIt
        List<DocumentNode> documents =
                DocumentController.getSelectedDocuments(neo4jAL, configurationName);
        for (DocumentNode d : documents) {
            try {
                List<Node> res = d.execute(applicationLabel); // Results need to be processed
            } catch (Exception | Neo4jNoResult | Neo4jBadRequestException err) {
                neo4jAL
                        .getLogger()
                        .error(
                                "An error occurred during Document request execution. Tag with Node ID : "
                                        + d.getNodeId(),
                                err);
            }
        }

        return nExecution;
    }
}
