package com.castsoftware.tagging.models;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
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

    @Override
    protected Node findNode() throws Neo4jBadRequest, Neo4jNoResult {
        String initQuery = String.format("MATCH (n:%s) WHERE ID(n)=%d RETURN n as node LIMIT 1;", LABEL, this.getNodeId());
        try {
            Result res = neo4jAL.executeQuery(initQuery);
            Node n = (Node) res.next().get("node");

            this.setNode(n);
            return n;
        } catch ( Neo4jQueryException e) {
            throw new Neo4jBadRequest("Company node initialization failed", initQuery , e, ERROR_PREFIX+"FIN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult("You need to create the ConfigurationNode node first.",  initQuery, e, ERROR_PREFIX+"FIN2");
        }
    }

    @Override
    public Node createNode() throws Neo4jBadRequest, Neo4jNoResult {
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
            throw new Neo4jBadRequest(LABEL + " node creation failed", queryDomain , e, ERROR_PREFIX+"CRN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  queryDomain, e, ERROR_PREFIX+"CRN2");
        }
    }

    public static List<ConfigurationNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequest {
        try {
            List<ConfigurationNode> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();
                    String name = (String) node.getProperty(NAME_PROPERTY);
                    // Initialize the node
                    ConfigurationNode cn = new ConfigurationNode(neo4jAL, name);
                    cn.setNode(node);

                    resList.add(cn);
                }  catch (NoSuchElementException |
                        NullPointerException e) {
                    throw new Neo4jNoResult(LABEL + " nodes retrieving failed",  "findQuery", e, ERROR_PREFIX+"GAN1");
                }
            }
            return resList;
        } catch (Neo4jQueryException | Neo4jNoResult e) {
            throw new Neo4jBadRequest(LABEL + " nodes retrieving failed", "findQuery" , e, ERROR_PREFIX+"GAN1");
        }
    }

    @Override
    public void deleteNode() throws Neo4jBadRequest {
        String queryDomain = String.format("MATCH (p:%s) WHERE ID(p)=%d DETACH DELETE p;",
                LABEL, this.getNodeId());
        try {
            neo4jAL.executeQuery(queryDomain);
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequest(LABEL + " node deletion failed", queryDomain , e, ERROR_PREFIX+"DEL1");
        }
    }

    public ConfigurationNode(Neo4jAL nal, String name) {
        super(nal);
        this.name = name;
    }

}
