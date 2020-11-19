package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.DocumentNode;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.models.UseCaseNode;
import com.castsoftware.tagging.statistics.PostStatisticsLogger;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ConfigurationController {

    private static final String ERROR_PREFIX = "CONFCx";
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");
    private static final String TAG_RETURN_LABEL_VAL = Configuration.get("tag.anchors.return.return_val");


    /**
     * Create a new configuration node
     * @param neo4jAL Neo4j Access Layer
     * @param name Name of the new configuration
     * @return The configuration node created
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public static Node createConfiguration(Neo4jAL neo4jAL, String name) throws Neo4jBadRequestException, Neo4jNoResult {
        ConfigurationNode n = new ConfigurationNode(neo4jAL, name);
        return n.createNode();
    }

    /**
     * Delete a configuration and its associated nodes.
     * @param neo4jAL Neo4J Access Layer
     * @param id ID of the configuration node to delete
     * @return The number of nodes deleted
     * @throws Neo4jQueryException
     */
    public static Long deleteConfiguration(Neo4jAL neo4jAL, Long id) throws Neo4jQueryException {
        String initQuery = String.format("MATCH p=(n:%s)-[:%s*]->(:%s)-[:%s*]->(:%s) WHERE ID(n)=%d return COUNT(p) as deleted_node",
                ConfigurationNode.getLabel(), USE_CASE_RELATIONSHIP, UseCaseNode.getLabel(), USE_CASE_RELATIONSHIP, TagNode.getLabel(), id);
        try {
            Result res = neo4jAL.executeQuery(initQuery);
            return (Long) res.next().get("deleted_node");
        } catch (NoSuchElementException ex) {
            throw new Neo4jQueryException(String.format("Cannot delete the configuration with ID : %d", id), initQuery , ex,  ERROR_PREFIX + "DELC1");
        }
    }

    /**
     * Execute the actual configuration
     * @param neo4jAL Neo4J Access layer
     * @param configurationName Name of the configuration to execute
     * @param applicationLabel Label of the application on which the tag will be applied
     * @throws ProcedureException
     * @return Number of tag applied in the configuration
     */
    public static int executeConfiguration(Neo4jAL neo4jAL, String configurationName, String applicationLabel) throws Neo4jBadRequestException, Neo4jQueryException, Neo4jNoResult {
        PostStatisticsLogger fl  = PostStatisticsLogger.getLogger();
        List<Label> labels = neo4jAL.getAllLabels();

        // Verify if the label is present in the database
        if(!labels.contains(Label.label(applicationLabel))) {
            String message = String.format("Cannot find label \"%s\" in the database", applicationLabel);
            throw new Neo4jBadRequestException(message, ERROR_PREFIX + "EXEC1");
        }

        int nExecution = 0;

        // Execute activated tag's requests
        List<TagNode> tags = TagController.getSelectedTags(neo4jAL, configurationName);

        for(TagNode n : tags) {
            try {
                List<Node> res = n.executeRequest(applicationLabel); // Results need to be processed
                neo4jAL.logInfo("Statistics saved for tag : " + n.getTag());
                nExecution ++;
            } catch (Exception | Neo4jNoResult | Neo4jBadRequestException err) {
                neo4jAL.getLogger().error("An error occurred during Tag request execution. Tag with Node ID : " + n.getNodeId(), err);
            }
        }

        // Execute DocumentIt
        List<DocumentNode> documents = DocumentController.getSelectedDocuments(neo4jAL, configurationName);
        for(DocumentNode d : documents) {
            try {
                List<Node> res = d.execute(applicationLabel); // Results need to be processed
            } catch (Exception | Neo4jNoResult | Neo4jBadRequestException err) {
                neo4jAL.getLogger().error("An error occurred during Document request execution. Tag with Node ID : " + d.getNodeId(), err);
            }
        }


        /*try {
            fl.write();
        } catch (IOException err) {
            neo4jAL.getLogger().error("Failed to save statistics during request execution.", err);
        }*/

        return nExecution;
    }

}
