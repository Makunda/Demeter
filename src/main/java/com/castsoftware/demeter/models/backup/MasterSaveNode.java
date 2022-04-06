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

package com.castsoftware.demeter.models.backup;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Master save node
 * The member are public by choice ( to be returned as a Neo4j value)
 */
public class MasterSaveNode {

    public static final String MASTERSAVE_NODE_LABEL = "DemeterMasterSave";

    private Long id;
    private String name;
    private String description;
    private Long timestamp;
    private String picture;

    private Node node;

    /**
     * Master Save Constructor
     *
     * @param name        Name of the backup
     * @param description Description
     * @param timestamp   Timestamp
     * @param picture     Picture
     */
    public MasterSaveNode(String name, String description, Long timestamp, String picture) {
        this.node = null;
        this.id = -1L;
        this.name = name;
        this.description = description;
        this.timestamp = timestamp;
        this.picture = picture;
    }


    /**
     * Master Save Constructor
     *
     * @param name Name of the backup
     */
    public MasterSaveNode(String name) {
        this.node = null;
        this.id = -1L;
        this.name = name;
        this.description = "";
        this.timestamp = new Date().getTime();
        this.picture = "";
    }

    /**
     * Constructor from a node
     *
     * @param node Node to treat
     * @throws Neo4jBadNodeFormatException
     */
    public MasterSaveNode(Node node) throws Neo4jBadNodeFormatException {
        try {
            this.node = node;
            this.id = (Long) node.getId();
            this.name = (String) node.getProperty("Name");
            this.description = node.hasProperty("Description") ? (String) node.getProperty("Description") : "";
            this.timestamp = node.hasProperty("Timestamp") ? (Long) node.getProperty("Timestamp") : 0L;
            this.picture = node.hasProperty("Picture") ? (String) node.getProperty("Picture") : "";
        } catch (Exception e) {
            // Detach delete the node
            node.getRelationships().forEach(Relationship::delete); // Detach
            node.delete(); // Delete
            throw new Neo4jBadNodeFormatException(String.format("The MasterSaveNode with id [%d] is not in a correct format", node.getId()), "MASTxCONS01");
        }
    }

    public Long getId() {
        return id;
    }

    public String getPicture() {
        return picture;
    }

    /**
     * Set a new picture format
     *
     * @param picture value
     */
    public void setPicture(String picture) {
        this.picture = picture;
        if (node != null) node.setProperty("Picture", picture);
    }

    public String getName() {
        return name;
    }

    /**
     * Set the name
     *
     * @param name value
     */
    public void setName(String name) {
        this.name = name;
        if (node != null) node.setProperty("Name", name);
    }

    public String getDescription() {
        return description;
    }

    /**
     * Set the description
     *
     * @param description value
     */
    public void setDescription(String description) {
        this.description = description;
        if (node != null) node.setProperty("Description", description);
    }

    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp value
     *
     * @param timestamp value
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        if (node != null) node.setProperty("Timestamp", timestamp);
    }

    public Optional<Node> getNode() {
        return Optional.ofNullable(this.node);
    }

    /**
     * Create the node for a specific application
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @return The create node
     * @throws Exception
     */
    public Node createNode(Neo4jAL neo4jAL, String application) throws Exception {
        // Create request
        String request = String.format("CREATE (o:`%s`:`%s`) " +
                "SET o.Name=$name " +
                "SET o.Description=$description " +
                "SET o.Timestamp=$timestamp " +
                "SET o.Picture=$picture " +
                "RETURN o as node;", application, MASTERSAVE_NODE_LABEL);
        Map<String, Object> params = Map.of(
                "name", this.name,
                "description", this.description,
                "timestamp", this.timestamp,
                "picture", this.picture
        );

        try {
            // Create a new node in the database and throw an error if it doesn't exist
            Node n;
            Result res = neo4jAL.executeQuery(request, params);
            if (res.hasNext()) n = (Node) res.next().get("node");
            else throw new Exception("Failed to create the Master Save node. No results.");

            return n;
        } catch (Neo4jQueryException | Exception err) {
            neo4jAL.logError(String.format("Failed to create the Master Save. Request : '%s'.", request), err);
            throw new Exception(String.format("Failed to create the node with name '%s' in application '%s'.", application, name));
        }
    }

    /**
     * Create the node and attach it to the object
     *
     * @param neo4jAL     Neo4j Access layer
     * @param application Name of the application
     * @return
     * @throws Exception
     */
    public MasterSaveNode create(Neo4jAL neo4jAL, String application) throws Exception {
        this.node = this.createNode(neo4jAL, application);
        return this;
    }
}
