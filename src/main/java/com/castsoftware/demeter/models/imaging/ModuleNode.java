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

package com.castsoftware.demeter.models.imaging;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.BackupNode;
import com.castsoftware.demeter.models.Neo4jObject;
import com.castsoftware.demeter.models.demeter.TagNode;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ModuleNode extends Neo4jObject {

    // Static Imaging properties
    private static final String LABEL = Configuration.get("imaging.node.module.label");
    private static final String IMAGING_MODULE_AIP_ID = Configuration.get("imaging.node.module.aipId");
    private static final String IMAGING_MODULE_COLOR = Configuration.get("imaging.node.module.color");
    private static final String IMAGING_MODULE_COUNT = Configuration.get("imaging.node.module.count");
    private static final String IMAGING_MODULE_NAME = Configuration.get("imaging.node.module.name");
    private static final String IMAGING_MODULE_TYPE = Configuration.get("imaging.node.module.type");

    // Static Imaging relationships
    private static final String IMAGING_LINKS_TO_OBJECTS = Configuration.get("imaging.node.module.links.to_objects");
    private static final String IMAGING_LINKS_TO_MODULES = Configuration.get("imaging.node.module.links.to_modules");
    private static final String ERROR_PREFIX = "MODNx";

    // Properties
    private String aipID;
    private String color;
    private Long count;
    private String name;
    private String type;

    // Static getters
    public static String getLabelProperty() {
        return LABEL;
    }
    public static String getAipIdProperty() {
        return IMAGING_MODULE_AIP_ID;
    }
    public static String getColorProperty() {
        return IMAGING_MODULE_COLOR;
    }
    public static String getCountProperty() {
        return IMAGING_MODULE_COUNT;
    }
    public static String getNameProperty() {
        return IMAGING_MODULE_NAME;
    }
    public static String getTypeProperty() {
        return IMAGING_MODULE_TYPE;
    }
    public static String getLinksToObjectsProperty() {
        return IMAGING_LINKS_TO_OBJECTS;
    }
    public static String getLinksToModulesProperty() {
        return IMAGING_LINKS_TO_MODULES;
    }

    // Getters
    public String getAipID() {
        return aipID;
    }
    public String getColor() {
        return color;
    }
    public Long getCount() {
        return count;
    }
    public String getName() {
        return name;
    }
    public String getType() {
        return type;
    }

    /**
     * Create a Module Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>Module</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static ModuleNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {
        if(!node.hasLabel(Label.label(getLabelProperty()))) {
            throw new Neo4jBadNodeFormatException(String.format("The node with Id '%d' does not contain the correct label. Expected to have : %s", node.getId(), LABEL), ERROR_PREFIX + "FROMN1");
        }

        try {
            // Initialize the node
            String aipID = (String) node.getProperty(getAipIdProperty());
            String color = (String) node.getProperty(getColorProperty());
            String name = (String) node.getProperty(getNameProperty());
            String type = (String) node.getProperty(getTypeProperty());

            // Optional parameters
            Long count = 0L;
            try {
                count = (Long) node.getProperty(getCountProperty());
            } catch (NotFoundException ignored) {
            }


            ModuleNode moduleNode = new ModuleNode(neo4jAL, aipID, color, count, name, type);
            moduleNode.setNode(node);

            return moduleNode;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            neo4jAL.logError("Error during Module node creation...", e);
            throw new Neo4jBadNodeFormatException(getLabelProperty() + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }


    @Override
    public Node createNode() throws Neo4jNoResult {

        try {
            Transaction tx = neo4jAL.getTransaction();
            Node n = tx.createNode(Label.label(LABEL));

            // Document properties
            n.setProperty(getAipIdProperty(), getName());
            n.setProperty(getColorProperty(), getColor());
            n.setProperty(getCountProperty(), getCount());
            n.setProperty(getNameProperty(), getName());
            n.setProperty(getTypeProperty(), getType());

            this.setNode(n);
            return n;
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  "", e, ERROR_PREFIX+"CRN2");
        }
    }

    /**
     * Return the merge Request associated with this node properties
     * @return The merge request as a String
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public String toMergeRequest(String applicationContext) throws Neo4jNoResult, Neo4jQueryException {
        Node n = getNode();

        // The count property can't be part of the merge request, due to its variable number 'Count'
        Map<String, Object> properties = n.getAllProperties();
        properties.remove(getCountProperty());

        // Add application label
        String forgedLabel = applicationContext.concat(":").concat(LABEL);

        return buildMergeRequest(forgedLabel, properties);
    }


    /**
     * Create a backup node associated with this module node
     * @param applicationContext Context where the backup will be executed
     * @return The Backup node created
     * @throws Neo4jBadRequestException The Backup request contains forbidden argument that led to an error
     * @throws Neo4jNoResult Was not able to backup
     * @throws Neo4jQueryException The backup failed due to abad query
     */
    public Node createBackup(String applicationContext, List<Node> affectedNodes) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        neo4jAL.logInfo(String.format("Creating a backup node for module with name '%s'. ", getName()));
        return BackupNode.createBackup(neo4jAL, applicationContext, getNode(), toMergeRequest(applicationContext), affectedNodes);
    }

    /**
     * Return all Level5Node node in the database
     * @param neo4jAL Neo4j Access Layer
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException If the request failed to execute
     */
    public static List<ModuleNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jNoResult {
        Label label = Label.label(LABEL);
        List<ModuleNode> returnList = new ArrayList<>();

        for (ResourceIterator<Node> it = neo4jAL.getTransaction().findNodes(label); it.hasNext(); ) {
            try {
                returnList.add(fromNode(neo4jAL, it.next()));
            }  catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                throw new Neo4jNoResult(LABEL + "nodes retrieving by application name failed",  "findQuery", e, ERROR_PREFIX+"GANA1");
            }

        }

        return returnList;
    }

    @Override
    public void deleteNode() throws Neo4jBadRequestException {
        String queryDomain = String.format("MATCH (p:%s) WHERE ID(p)=%d DETACH DELETE p;",
                LABEL, this.getNodeId());
        try {
            neo4jAL.executeQuery(queryDomain);
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node deletion failed", queryDomain , e, ERROR_PREFIX+"DEL1");
        }
    }

    public ModuleNode(Neo4jAL neo4jAL, String aipID, String color, Long count, String name, String type) {
        super(neo4jAL);
        this.aipID = aipID;
        this.color = color;
        this.count = count;
        this.name = name;
        this.type = type;
    }
}
