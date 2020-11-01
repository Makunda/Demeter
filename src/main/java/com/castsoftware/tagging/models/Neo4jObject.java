package com.castsoftware.tagging.models;

import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import org.neo4j.graphdb.Node;

public abstract class Neo4jObject {

    private Node node = null;
    protected Neo4jAL neo4jAL;

    @Deprecated
    public String neo4jSanitize(String s) {
        return s.replaceAll("\\\\", "\\\\\\\\");
    }

    /**
     * Find an existing node in the DB matching the same properties. The node found will be assigned to the object.
     * (Warning : will return the first node encountered if two nodes have the exact same properties)
     * @return The node found in the database
     * @throws Neo4jBadRequest An error was thrown during the execution of the query
     * @throws Neo4jNoResult The request did not return any results (and was supposed to)
     */
    protected abstract Node findNode() throws Neo4jBadRequest, Neo4jNoResult;

    /**
     * Create a node in the database based on attributes of the object
     * @return The node created
     * @throws Neo4jBadRequest An error was thrown during the execution of the query
     * @throws Neo4jNoResult The request did not return any results (and was supposed to)
     */
    public abstract Node createNode() throws Neo4jBadRequest, Neo4jNoResult;

    /**
     * Delete the node in the database linked to the object
     * @throws Neo4jBadRequest An error was thrown during the execution of the query
     * @throws Neo4jNoResult The request did not return any results (and was supposed to)
     */
    public abstract void deleteNode() throws Neo4jBadRequest, Neo4jNoResult;

    /**
     * Return the ID of the Neo4j node in the database
     * @return <code>Long</code> ID of the node
     */
    public Long getNodeId() {
        if(this.node == null) return null;
        return node.getId();
    }

    public Node getNode() throws Neo4jBadRequest, Neo4jNoResult {
        if(this.node == null) {
            this.findNode();
        }
        return this.node;
    }

    /**
     * Force an object to use a node already present in the database.
     */
    public void setNode(Node n) {
        this.node = n;
    }

    public Neo4jObject(Neo4jAL neo4jAL) {
        this.neo4jAL = neo4jAL;
    }
}
