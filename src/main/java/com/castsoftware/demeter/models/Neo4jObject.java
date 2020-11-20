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

package com.castsoftware.demeter.models;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import org.neo4j.graphdb.Node;

public abstract class Neo4jObject {

    private Node node = null;
    protected Neo4jAL neo4jAL;

    @Deprecated
    public String neo4jSanitize(String s) {
        return s.replaceAll("\\\\", "\\\\\\\\");
    }

    /**
     * Parse the specified node property and extract the boolean value.
     * During manipulations of the property, some process can accidentally convert the value of the boolean to a text format.
     * When this happens, a classical boolean cast fails to execute and throw ClassCastException.
     * @param value Object containing the boolean
     * @return The value of the boolean. If no boolean is detected, return false.
     */
    public static Boolean castPropertyToBoolean(Object value) {
        boolean b = false;

        try {
            b = (Boolean) value;
        } catch (ClassCastException e) {
            String boolAsString = (String) value;
            if(boolAsString.matches("true|false")) {
                b =  Boolean.parseBoolean(boolAsString);
            }
        }

        return b;
    }

    /**
     * Find an existing node in the DB matching the same properties. The node found will be assigned to the object.
     * (Warning : will return the first node encountered if two nodes have the exact same properties)
     * @return The node found in the database
     * @throws Neo4jBadRequestException An error was thrown during the execution of the query
     * @throws Neo4jNoResult The request did not return any results (and was supposed to)
     */
    protected abstract Node findNode() throws Neo4jBadRequestException, Neo4jNoResult;

    /**
     * Create a node in the database based on attributes of the object
     * @return The node created
     * @throws Neo4jBadRequestException An error was thrown during the execution of the query
     * @throws Neo4jNoResult The request did not return any results (and was supposed to)
     */
    public abstract Node createNode() throws Neo4jBadRequestException, Neo4jNoResult;

    /**
     * Delete the node in the database linked to the object
     * @throws Neo4jBadRequestException An error was thrown during the execution of the query
     * @throws Neo4jNoResult The request did not return any results (and was supposed to)
     */
    public abstract void deleteNode() throws Neo4jBadRequestException, Neo4jNoResult;

    /**
     * Return the ID of the Neo4j node in the database
     * @return <code>Long</code> ID of the node
     */
    public Long getNodeId() {
        if(this.node == null) return null;
        return node.getId();
    }

    /**
     * Return the node linked to the Object
     * @return <code>Node</code> node associated to the
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public Node getNode() throws Neo4jBadRequestException, Neo4jNoResult {
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
