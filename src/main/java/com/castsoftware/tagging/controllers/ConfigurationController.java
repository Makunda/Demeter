package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.TagRequestNode;
import com.castsoftware.tagging.models.UseCaseNode;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.NoSuchElementException;

public class ConfigurationController {

    private static final String ERROR_PREFIX = "CONFCx";
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");

    public static Node createConfiguration(Neo4jAL neo4jAL, String name) throws Neo4jBadRequest, Neo4jNoResult {
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
                ConfigurationNode.getLabel(), USE_CASE_RELATIONSHIP, UseCaseNode.getLabel(), USE_CASE_RELATIONSHIP, TagRequestNode.getLabel(), id);
        try {
            Result res = neo4jAL.executeQuery(initQuery);
            return (Long) res.next().get("deleted_node");
        } catch (NoSuchElementException ex) {
            throw new Neo4jQueryException(String.format("Cannot delete the configuration with ID : %d", id), initQuery , ex,  ERROR_PREFIX + "DELC1");
        }

    }

}
