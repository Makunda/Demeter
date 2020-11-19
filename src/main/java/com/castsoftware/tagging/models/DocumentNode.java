package com.castsoftware.tagging.models;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.*;
import com.castsoftware.tagging.tags.TagProcessing;
import com.castsoftware.tagging.tools.DocumentItGenerator;
import org.neo4j.graphdb.*;

import java.util.*;

public class DocumentNode extends Neo4jObject {

    private static final String LABEL = Configuration.get("neo4j.nodes.t_documentTag");
    private static final String INDEX_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.index");
    private static final String TITLE_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.title");
    private static final String ACTIVE_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.active");
    private static final String DESCRIPTION_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.description");
    private static final String REQUEST_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.request");
    private static final String DOCUMENT_DESCRIPTION_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.doc_description");
    private static final String ERROR_PREFIX = Configuration.get("neo4j.nodes.t_tag_node.error_prefix");

    private static final String USECASE_TO_TAG_REL = Configuration.get("neo4j.nodes.t_documentTag.error_prefix");

    // Anchors
    private static final String RETURN_ANCHOR = Configuration.get("tag.anchors.return.return_val");
    private static final String COUNT_RETURN_VAL = Configuration.get("tag.anchors.countReturn.return_val");

    // Variables
    private String title;
    private String request;
    private Boolean active;
    private String description;
    private String documentDescription;

    // Static getters
    public static String getLabel() {
        return LABEL;
    }
    public static String getIndexProperty() {
        return INDEX_PROPERTY;
    }
    public static String getTitleProperty() {
        return TITLE_PROPERTY;
    }
    public static String getActiveProperty() {
        return ACTIVE_PROPERTY;
    }
    public static String getRequestProperty() {
        return  REQUEST_PROPERTY;
    }
    public static String getDescriptionProperty() {
        return  DESCRIPTION_PROPERTY;
    }
    public static String getDocumentDescriptionProperty() {
        return  DOCUMENT_DESCRIPTION_PROPERTY;
    }

    // Getters
    public String getTitle() {
        return title;
    }
    public String getRequest() {
        return request;
    }
    public Boolean getActive() {
        return active;
    }
    public String getDescription() {
        return description;
    }
    public String getDocumentDescription() {
        return documentDescription;
    }

    /**
     * Create a DocumentTag Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>ConfigurationNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static DocumentNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {

        if(!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException(String.format("The node with Id '%d' does not contain the correct label. Expected to have : %s", node.getId(), LABEL), ERROR_PREFIX + "FROMN1");
        }

        try {
            String name = (String) node.getProperty(TITLE_PROPERTY);

            // Initialize the node
            String title = (String) node.getProperty(DocumentNode.getTitleProperty());
            String request = (String) node.getProperty(DocumentNode.getRequestProperty());
            boolean active = Neo4jObject.castPropertyToBoolean(node.getProperty(DocumentNode.getActiveProperty()));
            String description = (String) node.getProperty(DocumentNode.getDescriptionProperty());
            String documentDescription = (String) node.getProperty(DocumentNode.getDocumentDescriptionProperty());

            DocumentNode docn = new DocumentNode(neo4jAL, title, request, active, description, documentDescription);
            docn.setNode(node);

            return docn;
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
            n.setProperty(TITLE_PROPERTY, getTitleProperty());
            n.setProperty(DOCUMENT_DESCRIPTION_PROPERTY, getDocumentDescription());

            // Tag Description
            n.setProperty(DESCRIPTION_PROPERTY, getDescription());

            // Procedure parameters
            n.setProperty(ACTIVE_PROPERTY, getActive());
            n.setProperty(REQUEST_PROPERTY, getRequest());

            this.setNode(n);
            return n;
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  "", e, ERROR_PREFIX+"CRN2");
        }
    }

    /**
     * Get the parent useCase Node attached to this Document node
     * @return The parent UseCase node
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     * @throws Neo4jBadNodeFormatException
     */
    public UseCaseNode getParentUseCase() throws Neo4jBadRequestException, Neo4jNoResult, Neo4jBadNodeFormatException {
        RelationshipType relName = RelationshipType.withName(USECASE_TO_TAG_REL);

        Node n = getNode();
        Relationship parentRel = n.getSingleRelationship(relName, Direction.INCOMING);

        if(parentRel != null) {
            Node useCase = parentRel.getStartNode();
            return UseCaseNode.fromNode(this.neo4jAL, useCase);
        } else {
            return null;
        }
    }

    /**
     * Return all Document node in the database
     * @param neo4jAL Neo4j Access Layer
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException
     */
    public static List<StatisticNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<StatisticNode> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();

                    StatisticNode trn = StatisticNode.fromNode(neo4jAL, node);
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
     * Execute the Document node and create the associated document on imaging
     * @param applicationLabel
     * @return <code>List<Node></code> the list of node concerned by the document creation
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public List<Node> execute(String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult {
        if(this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX+"EXEC1");

        try {
            String forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.processAll(forgedReq);

            Result res = neo4jAL.executeQuery(forgedReq);

            List<Node> toDocNode = new ArrayList<>();
            while(res.hasNext()) {
                try {
                    Node n = (Node) res.next().get(RETURN_ANCHOR);
                    toDocNode.add(n);
                } catch (Exception ignored) {
                    // Ignored
                }
            }

            DocumentItGenerator.create(neo4jAL, title, description, toDocNode, null);
            return toDocNode;

        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            throw new Neo4jBadRequestException("The request failed to execute.", this.request, e, ERROR_PREFIX+"EXEC2");
        }
    }

    /**
     * Launch the request against an application, without tagging the results
     * @param applicationLabel Label of the application
     * @return The Number of node concerned by the document
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public Long forecastRequest(String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult {
        if(this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX+"EXEC1");

        try {
            String forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.forgeCountRequest(forgedReq);

            Result res = neo4jAL.executeQuery(forgedReq);

            Long numAffected = 0L;
            if(res.hasNext()) {
                numAffected = (Long) res.next().get(COUNT_RETURN_VAL);
            }

            return numAffected;

        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            throw new Neo4jBadRequestException("The request failed to execute.", this.request, e, ERROR_PREFIX+"EXEC2");
        }
    }

    public DocumentNode(Neo4jAL neo4jAL, String title, String request, Boolean active, String description, String documentDescription) {
        super(neo4jAL);
        this.title = title;
        this.request = request;
        this.active = active;
        this.description = description;
        this.documentDescription = documentDescription;
    }
}
