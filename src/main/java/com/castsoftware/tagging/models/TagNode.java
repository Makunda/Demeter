package com.castsoftware.tagging.models;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.*;
import com.castsoftware.tagging.tags.TagProcessing;
import org.neo4j.graphdb.*;

import java.util.*;

public class TagNode extends Neo4jObject{

    // Configuration properties
    private static final String LABEL = Configuration.get("neo4j.nodes.t_tag_node");
    private static final String TAG_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.tag");
    private static final String REQUEST_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.request");
    private static final String ACTIVE_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.active");
    private static final String ERROR_PREFIX = Configuration.get("neo4j.nodes.t_tag_node.error_prefix");

    private static final String LABEL_ANCHOR = Configuration.get("tag.anchors.label");

    // Node properties
    private String tag;
    private String request;
    private Boolean active;

    public static String getLabel() {
        return LABEL;
    }

    public static String getTagProperty() {
        return TAG_PROPERTY;
    }
    public static String getActiveProperty() { return ACTIVE_PROPERTY; }
    public static String getRequestProperty() { return  REQUEST_PROPERTY; }

    public String getTag() {
        return tag;
    }

    public String getRequest() {
        return request;
    }

    public Boolean getActive() {
        return active;
    }

    /**
     * Create a TagRequestNode Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>TagRequestNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static TagNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {

        if(!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException("The node does not contain the correct label. Expected to have : " + LABEL, ERROR_PREFIX + "FROMN1");
        }

        try {
            String tag = (String) node.getProperty(TAG_PROPERTY);

            boolean active = false;
            try {
                active = (Boolean) node.getProperty(UseCaseNode.getActiveProperty());

            } catch (ClassCastException e) {
                String boolAsString = (String) node.getProperty(UseCaseNode.getActiveProperty());
                if(boolAsString.matches("true|false")) {
                    active =  Boolean.parseBoolean(boolAsString);
                }
            }


            String request = (String) node.getProperty(REQUEST_PROPERTY);

            // Initialize the node
            TagNode trn = new TagNode(neo4jAL, tag, active, request);
            trn.setNode(node);

            return trn;
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
    public Node createNode() throws Neo4jBadRequestException, Neo4jNoResult {
        String queryDomain = String.format("MERGE (p:%s { %s : \"%s\", %s : \"%s\", %s : %b }) RETURN p as node;",
                LABEL, TAG_PROPERTY, tag, REQUEST_PROPERTY, request, ACTIVE_PROPERTY, this.active);
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

    public static List<TagNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<TagNode> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();

                    TagNode trn = TagNode.fromNode(neo4jAL, node);
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

    /**
     * Execute the request of the tag in a specific context.
     * @param applicationLabel The application that will be flagged by the request.
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public Result executeRequest(String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult {
        if(this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX+"EXEC1");

        // Build parameters
        Map<String,Object> params = new HashMap<>();
        params.put( "tagName", this.tag );

        try {
            String forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.processAll(forgedReq);

            neo4jAL.info("Request went from : " + this.request);
            neo4jAL.info("To  : " + forgedReq);

            return neo4jAL.executeQuery(forgedReq, params);
        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            throw new Neo4jBadRequestException("The request failed to execute.", this.request, e, ERROR_PREFIX+"EXEC2");
        }
    }

    /**
     * Test a query using "EXPLAIN" keyword in Neo4j. This function do not execute the query, but will produce an error if it's incorrect.
     * @param applicationLabel The application that will be flagged by the request.
     * @return <code>Boolean</code> True if the request is valid, false otherwise.
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public boolean checkQuery (String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult {
        if(this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX+"CHECK1");

        // Build parameters
        Map<String,Object> params = new HashMap<>();
        params.put( "tagName", this.tag );

        // Forge the request, remove first and last quotes
        String req = this.request.replace(LABEL_ANCHOR, applicationLabel);
        String forgedReq = "EXPLAIN " + req.replaceAll("(^\\s\")|(\\s\"\\s?$)", "");

        try (Transaction tx = neo4jAL.getDb().beginTx() ) {

            tx.execute(forgedReq, params);
            return true;
        } catch (QueryExecutionException e) {
            String m = String.format("The tag \"%s\" with associated request \"%s\" is not valid. Check Failed with error..", this.getTag(), this.getRequest());
            neo4jAL.getLogger().warn(m);
            return false;
        }

    }

    public TagNode(Neo4jAL nal, String tag, Boolean active, String request) {
        super(nal);
        this.tag = tag;
        this.active = active;
        this.request = request;
    }
}
