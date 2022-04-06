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
import com.castsoftware.demeter.database.Neo4jTypeManager;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.BackupNode;
import com.castsoftware.demeter.models.Neo4jObject;
import com.castsoftware.demeter.utils.configuration.LevelGroupingConfiguration;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Level5Node extends Neo4jObject {

    // Static Properties
    private static final String LABEL = Configuration.get("imaging.node.level5.label");
    private static final String HIDDEN_LABEL = Configuration.get("imaging.node.hidden.level.prefix");

    private static final String NAME_PROPERTY = Configuration.get("imaging.node.level5.name");
    private static final String CONCEPT_PROPERTY = Configuration.get("imaging.node.level5.concept");
    private static final String DRILL_DOWN_PROPERTY =
            Configuration.get("imaging.node.level5.alternateDrilldown");
    private static final String FULL_NAME_PROPERTY =
            Configuration.get("imaging.node.level5.fullName");
    private static final String COLOR_PROPERTY = Configuration.get("imaging.node.level5.color");
    private static final String LEVEL_PROPERTY = Configuration.get("imaging.node.level5.level");
    private static final String COUNT_PROPERTY = Configuration.get("imaging.node.level5.count");
    private static final String SHADE_PROPERTY = Configuration.get("imaging.node.level5.shade");

    private static final String LEVEL_LINKS = Configuration.get("imaging.node.level_nodes.links");

    private static final String ERROR_PREFIX = "LEV5Nx";

    // Properties
    private final String name;
    private final Boolean concept;
    private final Boolean drillDown;
    private final String fullName;
    private final String color;
    private final Long level;
    private final Long count;
    private final String shade;

    public Level5Node(
            Neo4jAL neo4jAL,
            String name,
            Boolean concept,
            Boolean drilldown,
            String fullname,
            String color,
            Long level,
            Long count,
            String shade) {
        super(neo4jAL);
        this.name = name;
        this.concept = concept;
        this.drillDown = drilldown;
        this.fullName = fullname;
        this.color = color;
        this.level = level;
        this.count = count;
        this.shade = shade;
    }

    // Static getters
    public static String getLabel() {
        return LABEL;
    }

    public static String getNameProperty() {
        return NAME_PROPERTY;
    }

    public static String getConceptProperty() {
        return CONCEPT_PROPERTY;
    }

    public static String getDrillDownProperty() {
        return DRILL_DOWN_PROPERTY;
    }

    public static String getFullNameProperty() {
        return FULL_NAME_PROPERTY;
    }

    public static String getColorProperty() {
        return COLOR_PROPERTY;
    }

    public static String getLevelProperty() {
        return LEVEL_PROPERTY;
    }

    public static String getCountProperty() {
        return COUNT_PROPERTY;
    }

    public static String getShadeProperty() {
        return SHADE_PROPERTY;
    }

    /**
     * Create a Level5 Node object from a neo4j node
     *
     * @param neo4jAL Neo4j Access Layer
     * @param node    Node associated to the object
     * @return <code>Level5Node</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or
     *                                     malformed property.
     */
    public static Level5Node fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {

        Label level5Label = Label.label(LABEL);

        if (!(node.hasLabel(level5Label))) {
            throw new Neo4jBadNodeFormatException(
                    String.format(
                            "The node with Id '%d' does not contain the correct label. Expected to have : %s ",
                            node.getId(), LABEL),
                    ERROR_PREFIX + "FROMN1");
        }

        try {
            // Initialize the node
            String name = (String) node.getProperty(getNameProperty());
            Boolean concept = Neo4jTypeManager.getAsBoolean(node, getConceptProperty(), false);
            String fullName = (String) node.getProperty(getFullNameProperty());
            String color = (String) node.getProperty(getColorProperty());
            Long level = Neo4jTypeManager.getAsLong(node, getLevelProperty(), 5L);
            String shade = (String) node.getProperty(getShadeProperty());

            // optional properties
            Long count = 0L;
            try {
                count = (Long) node.getProperty(getCountProperty());
            } catch (NotFoundException ignored) {
            }

            Boolean drillDown = false;
            try {
                drillDown = (Boolean) node.getProperty(getDrillDownProperty());
            } catch (NotFoundException ignored) {
            }

            Level5Node level5Node =
                    new Level5Node(neo4jAL, name, concept, drillDown, fullName, color, level, count, shade);
            level5Node.setNode(node);

            return level5Node;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            neo4jAL.logError("Error during level 5 node creation...", e);
            throw new Neo4jBadNodeFormatException(
                    LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }

    /**
     * Return all Level5Node node in the database
     *
     * @param neo4jAL Neo4j Access Layer
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException If the request failed to execute
     */
    public static List<Level5Node> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<Level5Node> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while (resIt.hasNext()) {
                try {
                    Node node = resIt.next();

                    Level5Node trn = Level5Node.fromNode(neo4jAL, node);
                    trn.setNode(node);

                    resList.add(trn);
                } catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                    throw new Neo4jNoResult(
                            LABEL + " nodes retrieving failed", "findQuery", e, ERROR_PREFIX + "GAN1");
                }
            }
            return resList;
        } catch (Neo4jQueryException | Neo4jNoResult e) {
            throw new Neo4jBadRequestException(
                    LABEL + " nodes retrieving failed", "findQuery", e, ERROR_PREFIX + "GAN1");
        }
    }

    /**
     * Return all Level5Node node present in one application
     *
     * @param neo4jAL         Neo4j Access Layer
     * @param applicationName Application name
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException If the request failed to execute
     */
    public static List<Level5Node> getAllNodesByApplication(Neo4jAL neo4jAL, String applicationName)
            throws Neo4jNoResult {
        Label label = Label.label(LABEL);
        Label applicationLabel = Label.label(applicationName);

        List<Level5Node> returnList = new ArrayList<>();
        Node n;
        for (ResourceIterator<Node> it = neo4jAL.getTransaction().findNodes(label); it.hasNext(); ) {
            try {
                // Check if the level node has the label of the application
                n = it.next();
                if (n.hasLabel(applicationLabel)) {
                    returnList.add(fromNode(neo4jAL, n));
                }
            } catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                throw new Neo4jNoResult(
                        LABEL + "nodes retrieving by application name failed",
                        "findQuery",
                        e,
                        ERROR_PREFIX + "GANA1");
            }
        }

        return returnList;
    }

    // Getters
    public String getName() {
        return name;
    }

    public Boolean getConcept() {
        return concept;
    }

    public Boolean getDrillDown() {
        return drillDown;
    }

    public String getFullName() {
        return fullName;
    }

    public String getColor() {
        return color;
    }

    public Long getLevel() {
        return level;
    }

    public Long getCount() {
        return count;
    }

    public String getShade() {
        return shade;
    }

    @Override
    public String toString() {
        return "Level5Node{"
                + "name='"
                + name
                + '\''
                + ", concept="
                + concept
                + ", drilldown="
                + drillDown
                + ", fullname='"
                + fullName
                + '\''
                + ", color='"
                + color
                + '\''
                + ", level="
                + level
                + ", count="
                + count
                + ", shade='"
                + shade
                + '\''
                + '}';
    }

    @Override
    public Node createNode() throws Neo4jNoResult {

        try {
            Transaction tx = neo4jAL.getTransaction();
            Node n = tx.createNode(Label.label(LABEL));

            // Document properties
            n.setProperty(getNameProperty(), getName());
            n.setProperty(getConceptProperty(), getConcept());
            n.setProperty(getDrillDownProperty(), getDrillDown());
            n.setProperty(getFullNameProperty(), getFullName());
            n.setProperty(getColorProperty(), getColor());
            n.setProperty(getLevelProperty(), getLevel());
            n.setProperty(getCountProperty(), getCount());
            n.setProperty(getShadeProperty(), getShade());

            this.setNode(n);
            return n;
        } catch (NoSuchElementException | NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed", "", e, ERROR_PREFIX + "CRN2");
        }
    }

    /**
     * Return the merge Request associated with this node properties
     *
     * @return The merge request as a String
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public String toMergeRequest(String applicationContext)
            throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        Node n = getNode();

        // The count property can't be part of the merge request, due to its variable number 'Count'
        Map<String, Object> properties = n.getAllProperties();
        properties.remove(Level5Node.getCountProperty());

        // Add application label
        String forgedLabel = applicationContext.concat(":").concat(LABEL);

        return buildMergeRequest(forgedLabel, properties);
    }

    /**
     * Create a backup node associated with this level node
     *
     * @param applicationContext
     * @return
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public Node createLevel5Backup(String applicationContext, List<Node> affectedNodes)
            throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        return BackupNode.createBackup(
                neo4jAL, applicationContext, getNode(), toMergeRequest(applicationContext), affectedNodes);
    }

    /**
     * Set the Auto generated property on this node
     */
    public void setAutoGeneratedProperty() throws Neo4jQueryException, Neo4jNoResult {
        Node n = getNode(); // Get the node
        n.setProperty(LevelGroupingConfiguration.getExtensionLevelIdentifier(), true); // Apply true
    }

    /**
     * Set the Auto generated property on this node
     * @param value Value of the property
     * @throws Neo4jQueryException
     * @throws Neo4jNoResult
     */
    public void setAutoGeneratedProperty(boolean value) throws Neo4jQueryException, Neo4jNoResult {
        Node n = getNode(); // Get the node
        n.setProperty(LevelGroupingConfiguration.getExtensionLevelIdentifier(), value); // Apply true
    }


}
