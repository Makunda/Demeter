package com.castsoftware.tagging.models;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class UseCaseNode extends Neo4jObject {

    // Configuration properties
    private final static String LABEL = Configuration.get("neo4j.nodes.t_use_case");
    private final static String INDEX_PROPERTY = Configuration.get("neo4j.nodes.t_use_case.index");
    private final static String NAME_PROPERTY = Configuration.get("neo4j.nodes.t_use_case.name");
    private final static String ACTIVE_PROPERTY = Configuration.get("neo4j.nodes.t_use_case.active");
    private final static String SELECTED_PROPERTY = Configuration.get("neo4j.nodes.t_use_case.selected");

    private final static String ERROR_PREFIX = Configuration.get("neo4j.nodes.t_use_case.error_prefix");

    // Node properties
    private String name;
    private Boolean active;
    private Boolean selected;

    public static String getLabel() {
        return LABEL;
    }

    public static String getActiveProperty() {
        return ACTIVE_PROPERTY;
    }
    public static String getNameProperty() {
        return NAME_PROPERTY;
    }
    public static String getSelectedProperty() { return SELECTED_PROPERTY; }

    public String getName() {
        return this.name;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Boolean getSelected() {
        return this.active;
    }

    /**
     * Create a UseCaseNode Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>UseCaseNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static UseCaseNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {

        if(!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException("The node does not contain the correct label. Expected to have : " + LABEL, ERROR_PREFIX + "FROMN1");
        }

        try {
            String name = (String) node.getProperty(NAME_PROPERTY);

            // Get and cast boolean is necessary
            boolean active = castPropertyToBoolean( node.getProperty(UseCaseNode.getActiveProperty()) );
            boolean selected = castPropertyToBoolean( node.getProperty(UseCaseNode.getSelectedProperty()) );

            // Initialize the node
            UseCaseNode ucn = new UseCaseNode(neo4jAL, name, active, selected);
            ucn.setNode(node);

            return ucn;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            neo4jAL.getLogger().error("Error during node instantiation. ", e);
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
            throw new Neo4jBadRequestException("Company node initialization failed", initQuery , e, ERROR_PREFIX+"FIN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult("You need to create the ConfigurationNode node first.",  initQuery, e, ERROR_PREFIX+"FIN2");
        }
    }

    @Override
    public Node createNode() throws Neo4jBadRequestException, Neo4jNoResult {
        String queryDomain = String.format("MERGE (p:%s { %s : '%s', %s : '%b' }) RETURN p as node;",
                LABEL, NAME_PROPERTY, this.name, ACTIVE_PROPERTY, this.active );
        try {

            Result res = neo4jAL.executeQuery(queryDomain);
            Node n = (Node) res.next().get("node");
            this.setNode(n);

            return n;
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node creation failed", queryDomain , e, ERROR_PREFIX+"CRN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  queryDomain, e, ERROR_PREFIX+"CRN2");
        }
    }

    public static List<UseCaseNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<UseCaseNode> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));

            int badFormattedNodes = 0;

            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();

                    // Initialize the node
                    UseCaseNode cn = UseCaseNode.fromNode(neo4jAL, node);

                    resList.add(cn);
                }  catch (Neo4jBadNodeFormatException e) {
                    badFormattedNodes ++;
                }
            }

            // Warn if nodes were omitted
            if(badFormattedNodes != 0) {
                String error = String.format("%d %s nodes were omitted due to a bad format.", badFormattedNodes, LABEL);
                neo4jAL.getLogger().warn(error);
            }

            return resList;
        } catch (Neo4jQueryException e) {
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

    public UseCaseNode(Neo4jAL nal, String name, Boolean active, Boolean selected) {
        super(nal);
        this.active = active;
        this.name = name;
        this.selected = selected;
    }

}
