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

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.*;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Level5Node extends Neo4jObject {

    // Static Properties
    private static final String LABEL = Configuration.get("imaging.node.level5.label");
    private static final String NAME_PROPERTY = Configuration.get("imaging.node.level5.name");
    private static final String CONCEPT_PROPERTY = Configuration.get("imaging.node.level5.concept");
    private static final String DRILLDOWN_PROPERTY = Configuration.get("imaging.node.level5.alternateDrilldown");
    private static final String FULLNAME_PROPERTY = Configuration.get("imaging.node.level5.fullName");
    private static final String COLOR_PROPERTY = Configuration.get("imaging.node.level5.color");
    private static final String LEVEL_PROPERTY = Configuration.get("imaging.node.level5.level5");
    private static final String COUNT_PROPERTY = Configuration.get("imaging.node.level5.count");
    private static final String SHADE_PROPERTY = Configuration.get("imaging.node.level5.shade");

    private static final String LEVEL_LINKS = Configuration.get("imaging.node.level_nodes.links");

    private static final String ERROR_PREFIX = "LEV5Nx";

    // Properties
    private String name;
    private Boolean concept;
    private Boolean drilldown;
    private String fullname;
    private String color;
    private Long level;
    private Long count;
    private String shade;

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
    public static String getDrilldownProperty() {
        return DRILLDOWN_PROPERTY;
    }
    public static String getFullnameProperty() {
        return FULLNAME_PROPERTY;
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

    // Getters
    public String getName() {
        return name;
    }
    public Boolean getConcept() {
        return concept;
    }
    public Boolean getDrilldown() {
        return drilldown;
    }
    public String getFullname() {
        return fullname;
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

    /**
     * Create a Level5 Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>ConfigurationNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static Level5Node fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {
        if(!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException(String.format("The node with Id '%d' does not contain the correct label. Expected to have : %s", node.getId(), LABEL), ERROR_PREFIX + "FROMN1");
        }

        try {
            // Initialize the node
            String name = (String) node.getProperty(getNameProperty());
            Boolean concept = (Boolean) node.getProperty(getConceptProperty());
            Boolean drilldown = (Boolean) node.getProperty(getDrilldownProperty());
            String fullname = (String) node.getProperty(getFullnameProperty());
            String color = (String) node.getProperty(getColorProperty());
            Long level = (Long) node.getProperty(getLevelProperty());
            Long count = (Long) node.getProperty(getCountProperty());
            String shade = (String) node.getProperty(getShadeProperty());

            Level5Node level5Node = new Level5Node(neo4jAL, name, concept, drilldown, fullname, color, level, count, shade);
            level5Node.setNode(node);

            return level5Node;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            throw new Neo4jBadNodeFormatException(LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }

    @Override
    protected Node findNode() throws Neo4jBadRequestException, Neo4jNoResult {
        String initQuery = String.format("MATCH (n:%s) WHERE ID(n)=%d RETURN n as node LIMIT 1;", LABEL, this.getNodeId());
        try {
            Result res = neo4jAL.executeQuery(initQuery);
            Node n = (Node) res.next().get("node");
            this.setNode(n);

            return n;
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node initialization failed", initQuery , e, ERROR_PREFIX+"FIN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(String.format("You need to create %s node first.", LABEL),  initQuery, e, ERROR_PREFIX+"FIN2");
        }
    }

    @Override
    public Node createNode() throws Neo4jNoResult {

        try {
            Transaction tx = neo4jAL.getTransaction();
            Node n = tx.createNode(Label.label(LABEL));

            // Document properties
            n.setProperty(getNameProperty(), getName());
            n.setProperty(getConceptProperty(), getConcept());
            n.setProperty(getDrilldownProperty(), getDrilldown());
            n.setProperty(getFullnameProperty(), getFullname());
            n.setProperty(getColorProperty(), getColor());
            n.setProperty(getLevelProperty(), getLevel());
            n.setProperty(getCountProperty(), getCount());
            n.setProperty(getShadeProperty(), getShade());

            this.setNode(n);
            return n;
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  "", e, ERROR_PREFIX+"CRN2");
        }
    }


    /**
     * Return all Level5Node node in the database
     * @param neo4jAL Neo4j Access Layer
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException If the request failed to execute
     */
    public static List<Level5Node> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<Level5Node> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();

                    Level5Node trn = Level5Node.fromNode(neo4jAL, node);
                    trn.setNode(node);

                    resList.add(trn);
                }  catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                    throw new Neo4jNoResult(LABEL + " nodes retrieving failed",  "findQuery", e, ERROR_PREFIX+"GAN1");
                }
            }
            return resList;
        } catch (Neo4jQueryException | Neo4jNoResult e) {
            throw new Neo4jBadRequestException(LABEL + " nodes retrieving failed", "findQuery" , e, ERROR_PREFIX+"GAN1");
        }
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

    public Level5Node(Neo4jAL neo4jAL, String name, Boolean concept, Boolean drilldown, String fullname, String color, Long level, Long count, String shade) {
        super(neo4jAL);
        this.name = name;
        this.concept = concept;
        this.drilldown = drilldown;
        this.fullname = fullname;
        this.color = color;
        this.level = level;
        this.count = count;
        this.shade = shade;
    }

    public Level5Node(Neo4jAL neo4jAL, Map<String, Object> properties) throws Neo4jBadNodeFormatException {
        super(neo4jAL);

        try {
            this.name = (String) properties.get(getNameProperty());
            this.concept = (Boolean) properties.get(getConceptProperty());
            this.drilldown = (Boolean) properties.get(getDrilldownProperty());
            this.fullname = (String) properties.get("FullName");
            this.color = (String) properties.get(getColorProperty());
            this.level = (Long) properties.get(getLevelProperty());
            this.count = (Long) properties.get(getCountProperty());
            this.shade = (String) properties.get(getShadeProperty());

        } catch (Exception e) {
            throw new Neo4jBadNodeFormatException("Failed to instantiate Level5 node.", e, ERROR_PREFIX+"CONS1");
        }

    }

}
