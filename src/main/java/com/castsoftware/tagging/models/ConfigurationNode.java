package com.castsoftware.tagging.models;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.*;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ConfigurationNode extends Neo4jObject {
    // Configuration properties
    private final static String LABEL = Configuration.get("neo4j.nodes.t_configuration");
    private final static String INDEX_PROPERTY = Configuration.get("neo4j.nodes.t_configuration.index");
    private final static String NAME_PROPERTY = Configuration.get("neo4j.nodes.t_configuration.name");

    private final static String ERROR_PREFIX = Configuration.get("neo4j.nodes.t_configuration.error_prefix");

    // Node properties
    private String name;

    public static String getLabel() {
        return LABEL;
    }

    public static String getNameProperty() {
        return NAME_PROPERTY;
    }

    /**
     * Create a Configuration Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>ConfigurationNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static ConfigurationNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {

        if(!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException("The node does not contain the correct label. Expected to have : " + LABEL, ERROR_PREFIX + "FROMN1");
        }

        try {
            String name = (String) node.getProperty(NAME_PROPERTY);

            // Initialize the node
            ConfigurationNode confn = new ConfigurationNode(neo4jAL, name);
            confn.setNode(node);

            return confn;
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
        } catch ( Neo4jQueryException e) {
            throw new Neo4jBadRequestException("Company node initialization failed", initQuery , e, ERROR_PREFIX+"FIN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult("You need to create the ConfigurationNode node first.",  initQuery, e, ERROR_PREFIX+"FIN2");
        }
    }

    @Override
    public Node createNode() throws Neo4jBadRequestException, Neo4jNoResult {
        neo4jAL.getLogger().info("Starting create node ");
        String queryDomain = String.format("CREATE (p:%s { %s : '%s' }) RETURN p as node;",
                LABEL, NAME_PROPERTY, name );
        try {
            Result res = neo4jAL.executeQuery(queryDomain);
            Node n = (Node) res.next().get("node");
            this.setNode(n);
            neo4jAL.getLogger().info("End create node req : " + queryDomain);
            return n;
        } catch ( Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node creation failed", queryDomain , e, ERROR_PREFIX+"CRN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  queryDomain, e, ERROR_PREFIX+"CRN2");
        }
    }

    public static List<ConfigurationNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<ConfigurationNode> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();

                    ConfigurationNode cn = ConfigurationNode.fromNode(neo4jAL, node);

                    resList.add(cn);
                }  catch ( Neo4jBadNodeFormatException e) {
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

    public ConfigurationNode(Neo4jAL nal, String name) {
        super(nal);
        this.name = name;
    }

}
