package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.*;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.UseCaseNode;
import org.neo4j.fabric.planning.Use;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;


public class UseCaseController {

    private static final String ERROR_PREFIX = "USECCx";
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");

    /**
     * Add a use case node to a configuration or another use case node
     * @param neo4jAL Neo4J Access Layer
     * @param name Name associated to the use case
     * @param active Activation status of the usecase
     * @param parentId Id of the parent node
     * @return the node associated to the use case created
     * @throws Neo4jQueryException An error happened during the execution of the query
     * @throws Neo4jBadRequest The request didn't returned the expected results
     * @throws Neo4jNoResult The request didn't return any result
     */
    public static Node addUseCase(Neo4jAL neo4jAL, String name, Boolean active, Long parentId) throws Neo4jQueryException, Neo4jBadRequest, Neo4jNoResult {
        Node parent = neo4jAL.getNodeById(parentId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());
        Label configLabel = Label.label(ConfigurationNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(useCaseLabel) && !parent.hasLabel(configLabel)) {
            throw new Neo4jBadRequest(String.format("Can only attach a %s node to a %s node or a %s node.", UseCaseNode.getLabel(),UseCaseNode.getLabel(), ConfigurationNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        UseCaseNode useCaseNode = new UseCaseNode(neo4jAL, name, active);
        Node n = useCaseNode.createNode();

        // Create the relation to the use case
        parent.createRelationshipTo(n, RelationshipType.withName(USE_CASE_RELATIONSHIP));

        return n;
    }

    /**
     * Return all use case nodes present in the database
     * @param neo4jAL Neo4J A
     * @return list of the use cases
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequest
     */
    public static List<UseCaseNode> listUseCases(Neo4jAL neo4jAL) throws Neo4jQueryException, Neo4jBadRequest {
        return UseCaseNode.getAllNodes(neo4jAL);
    }

    /**
     * Return all active use case nodes present in the database
     * @param neo4jAL Neo4J A
     * @return list of active use cases in the database
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequest
     */
    public static List<UseCaseNode> listActiveUseCases(Neo4jAL neo4jAL) throws Neo4jQueryException, Neo4jBadRequest {
        return UseCaseNode.getAllNodes(neo4jAL)
                .stream()
                .filter(UseCaseNode::getActive)
                .collect(Collectors.toList());
    }

    /**
     * Change the status of a use case node, and of every use case node under it.
     * @param neo4jAL Neo4j access layer
     * @param id Id of the use case to modify
     * @return list of the use case modified during the action
     * @throws Neo4jQueryException
     */
    public static List<UseCaseNode> activateUseCase(Neo4jAL neo4jAL, Long id, Boolean status) throws Neo4jQueryException, Neo4jBadRequest {

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        Node n = neo4jAL.getNodeById(id);

        // Check if the node provided if a Use Case node, otherwise throw an error
        if(!n.hasLabel(useCaseLabel))
            throw new Neo4jBadRequest("Node does not contain the require label : " + UseCaseNode.getLabel(), ERROR_PREFIX + "ACUSC1");


        Stack<Node> toVisit = new Stack<>();
        List<UseCaseNode> useCaseList = new ArrayList<>();

        toVisit.push(n);

        // Loop while node are discovered
        while(!toVisit.isEmpty()) {
            try {
                Node toTreat = toVisit.pop();
                toTreat.setProperty(UseCaseNode.getActiveProperty(), status);

                for(Relationship rel : toTreat.getRelationships(Direction.OUTGOING, RelationshipType.withName(USE_CASE_RELATIONSHIP))) {
                    Node otherNode = rel.getEndNode();

                    if(useCaseList.contains(otherNode) || !otherNode.hasLabel(useCaseLabel))
                        continue;

                    toVisit.add(otherNode);
                }

                useCaseList.add(UseCaseNode.fromNode(neo4jAL, toTreat));
            } catch (Neo4jBadNodeFormat e) {
                neo4jAL.getLogger().warn("Failed to create object for UseCase node", e);
            }
        }

        return useCaseList;
    }

}
