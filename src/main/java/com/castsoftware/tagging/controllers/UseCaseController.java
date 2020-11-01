package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.UseCaseNode;
import org.neo4j.graphdb.*;

import java.util.List;


public class UseCaseController {

    private static final String ERROR_PREFIX = "USECCx";
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");

    public static Node addUseCase(Neo4jAL neo4jAL, String name, Boolean active, Long parentId) throws Neo4jConnectionError, Neo4jQueryException, Neo4jBadRequest, Neo4jNoResult {
        // Check if the parent is either a Configuration Node or another use case
        Node parent = neo4jAL.getNodeById(parentId);
        Label UseCaseLabel = Label.label(UseCaseNode.getLabel());
        Label ConfigLabel = Label.label(ConfigurationNode.getLabel());

        if(!parent.hasLabel(UseCaseLabel) && !parent.hasLabel(ConfigLabel)) {
            throw new Neo4jBadRequest(String.format("Can only attach a %s node to a %s node or a %s node.", UseCaseNode.getLabel(),UseCaseNode.getLabel(), ConfigurationNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        UseCaseNode useCaseNode = new UseCaseNode(neo4jAL, name, active);
        Node n = useCaseNode.createNode();

        // Create the relation to the use case
        parent.createRelationshipTo(n, RelationshipType.withName(USE_CASE_RELATIONSHIP));

        return n;
    }

    public static List<UseCaseNode> listUseCases(Neo4jAL neo4jAL) throws Neo4jQueryException, Neo4jBadRequest {
        return UseCaseNode.getAllNodes(neo4jAL);
    }

    public static List<UseCaseNode> activateUseCase(Neo4jAL neo4jAL, Long id) throws Neo4jQueryException, Neo4jBadRequest {

        Node



    }

}
