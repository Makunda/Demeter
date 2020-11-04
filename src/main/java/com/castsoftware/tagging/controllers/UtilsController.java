package com.castsoftware.tagging.controllers;

import com.castsoftware.exporter.io.Exporter;
import com.castsoftware.exporter.io.Importer;
import com.castsoftware.exporter.results.OutputMessage;
import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadNodeFormat;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.models.UseCaseNode;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilsController {

    private static final String ERROR_PREFIX = "UTICx";
    private static final List<String> ALL_LABELS = Arrays.asList( ConfigurationNode.getLabel(), UseCaseNode.getLabel(), TagNode.getLabel());
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");
    private static final String USE_CASE_TO_TAG_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_tag");

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
        Exporter exporter = new Exporter(neo4jAL.getDb(), neo4jAL.getLogger());
        return exporter.save(ALL_LABELS, path, filename, true, false);
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
     * Return all the activated node matching an "activated" use case route ( a path of use case, with the "Activate" parameter, set on "on")
     * @param neo4jAL Neo4j access layer
     * @param configurationName Name of the configuration to use
     * @return The list of activated tags
     * @throws Neo4jQueryException
     * @throws Neo4jNoResult
     * @throws Neo4jBadRequest
     */
    public static List<TagNode> getActiveTag(Neo4jAL neo4jAL, String configurationName) throws Neo4jQueryException, Neo4jNoResult, Neo4jBadRequest {
        String req = String.format("MATCH(o:%s) WHERE o.%s=\"%s\" RETURN o as res", ConfigurationNode.getLabel(), ConfigurationNode.getNameProperty(), configurationName);

        Result result = neo4jAL.executeQuery(req);

        if(!result.hasNext()) {
            throw new Neo4jNoResult(String.format("The request to find Configuration node with name \"%s\" didn't produced any result.", configurationName), req, ERROR_PREFIX + "GATG1");
        }

        Node confNode = null;

        try {
            confNode = (Node) result.next().get("res");
        } catch (NoSuchElementException | NullPointerException e) {
            throw new Neo4jBadRequest("Error the request didn't return results in a correct format.", req, e, "GATG2");
        }

        // Iterate over Active Use Case
        Stack<Node> toVisit = new Stack<>();
        Set<Node> visited = new HashSet<>();
        Set<Node> tags = new HashSet<>();

        toVisit.add(confNode);

        while(!toVisit.isEmpty()) {
            Node n = toVisit.pop();

            // Check the activation value if useCase Node
            if(n.hasLabel( Label.label(UseCaseNode.getLabel())) ) {
                // Check the value for active property
                boolean b = (Boolean) n.getProperty(UseCaseNode.getActiveProperty());
                if(!b) {
                    visited.add(n);
                    continue;
                }
            }

            // Check if UseCase Nodes are connected
            for (Relationship rel : n.getRelationships(Direction.OUTGOING, RelationshipType.withName(USE_CASE_RELATIONSHIP))) {
                Node otherNode = rel.getEndNode();
                if(!visited.contains(otherNode)) {
                    toVisit.add(otherNode);
                }
            }

            // Check if Tag nodes are connected
            for (Relationship rel : n.getRelationships(Direction.OUTGOING, RelationshipType.withName(USE_CASE_TO_TAG_RELATIONSHIP))) {
                tags.add(rel.getEndNode());
            }

            visited.add(n);
        }


        //TagNode.fromNode(neo4jAL, otherNode)
        return tags.stream().map( x -> {
            try {
                return TagNode.fromNode(neo4jAL, x);
            } catch (Neo4jBadNodeFormat ex) {
                neo4jAL.getLogger().error("Error during Tag Nodes discovery.", ex);
                return null;
            }
        }).filter(x -> x != null && x.getActive()).collect(Collectors.toList());
    }

    /**
     * Execute the actual configuration
     * @param neo4jAL Neo4J Access layer
     * @param configurationName Name of the configuration to execute
     * @param applicationLabel Label of the application on which the tag will be applied
     * @throws ProcedureException
     * @return Number of tag applied in the configuration
     */
    public static int executeConfiguration(Neo4jAL neo4jAL, String configurationName, String applicationLabel) throws Neo4jBadRequest, Neo4jQueryException, Neo4jNoResult {

        List<Label> labels = neo4jAL.getAllLabels();

        // Verify if the label is present in the database
        if(!labels.contains(Label.label(applicationLabel))) {
            String message = String.format("Cannot find label \"%s\" in the database", applicationLabel);
            throw new Neo4jBadRequest(message, ERROR_PREFIX + "EXEC1");
        }

        int nExecution = 0;

        // Execute activated tags'requests
        List<TagNode> tags = getActiveTag(neo4jAL, configurationName);
        for(TagNode n : tags) {
            try {
                n.executeRequest(applicationLabel);
                nExecution ++;
            } catch (Neo4jNoResult | Neo4jBadRequest err) {
                neo4jAL.getLogger().error("An error occurred during Tag request execution. Tag with Node ID : " + n.getNodeId(), err);
            }
        }

        return nExecution;
    }

}
