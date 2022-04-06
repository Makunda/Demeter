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

package com.castsoftware.demeter.models.demeter;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.*;
import com.castsoftware.demeter.models.Neo4jObject;
import com.castsoftware.demeter.tags.TagProcessing;
import com.castsoftware.demeter.utils.DocumentItGenerator;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class DocumentNode extends Neo4jObject {

    private static final String LABEL = Configuration.get("neo4j.nodes.t_documentTag");
    private static final String INDEX_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.index");
    private static final String TITLE_PROPERTY = Configuration.get("neo4j.nodes.t_documentTag.title");
    private static final String ACTIVE_PROPERTY =
            Configuration.get("neo4j.nodes.t_documentTag.active");
    private static final String DESCRIPTION_PROPERTY =
            Configuration.get("neo4j.nodes.t_documentTag.description");
    private static final String REQUEST_PROPERTY =
            Configuration.get("neo4j.nodes.t_documentTag.request");
    private static final String DOCUMENT_DESCRIPTION_PROPERTY =
            Configuration.get("neo4j.nodes.t_documentTag.doc_description");
    private static final String ERROR_PREFIX =
            Configuration.get("neo4j.nodes.t_tag_node.error_prefix");

    private static final String USECASE_TO_TAG_REL =
            Configuration.get("neo4j.nodes.t_documentTag.error_prefix");

    // Anchors
    private static final String RETURN_ANCHOR = Configuration.get("tag.anchors.return.return_val");
    private static final String COUNT_RETURN_VAL =
            Configuration.get("tag.anchors.countReturn.return_val");

    // Variables
    private final String title;
    private final String request;
    private final Boolean active;
    private final String description;
    private final String documentDescription;

    public DocumentNode(
            Neo4jAL neo4jAL,
            String title,
            String request,
            Boolean active,
            String description,
            String documentDescription) {
        super(neo4jAL);
        this.title = title;
        this.request = request;
        this.active = active;
        this.description = description;
        this.documentDescription = documentDescription;
    }

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
        return REQUEST_PROPERTY;
    }

    public static String getDescriptionProperty() {
        return DESCRIPTION_PROPERTY;
    }

    public static String getDocumentDescriptionProperty() {
        return DOCUMENT_DESCRIPTION_PROPERTY;
    }

    /**
     * Create a DocumentTag Node object from a neo4j node
     *
     * @param neo4jAL Neo4j Access Layer
     * @param node    Node associated to the object
     * @return <code>ConfigurationNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or
     *                                     malformed property.
     */
    public static DocumentNode fromNode(Neo4jAL neo4jAL, Node node)
            throws Neo4jBadNodeFormatException {

        if (!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException(
                    String.format(
                            "The node with Id '%d' does not contain the correct label. Expected to have : %s",
                            node.getId(), LABEL),
                    ERROR_PREFIX + "FROMN1");
        }

        try {
            String name = (String) node.getProperty(TITLE_PROPERTY);

            // Initialize the node
            String title = (String) node.getProperty(DocumentNode.getTitleProperty());
            String request = (String) node.getProperty(DocumentNode.getRequestProperty());
            boolean active =
                    Neo4jObject.castPropertyToBoolean(node.getProperty(DocumentNode.getActiveProperty()));
            String description = (String) node.getProperty(DocumentNode.getDescriptionProperty());
            String documentDescription =
                    (String) node.getProperty(DocumentNode.getDocumentDescriptionProperty());

            DocumentNode docn =
                    new DocumentNode(neo4jAL, title, request, active, description, documentDescription);
            docn.setNode(node);

            return docn;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            throw new Neo4jBadNodeFormatException(
                    LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }

    /**
     * Return all Document node in the database
     *
     * @param neo4jAL Neo4j Access Layer
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException
     */
    public static List<DocumentNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jNoResult {
        Label label = Label.label(LABEL);
        List<DocumentNode> returnList = new ArrayList<>();

        for (ResourceIterator<Node> it = neo4jAL.getTransaction().findNodes(label); it.hasNext(); ) {
            try {
                returnList.add(fromNode(neo4jAL, it.next()));
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
        } catch (NoSuchElementException | NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed", "", e, ERROR_PREFIX + "CRN2");
        }
    }

    /**
     * Get the parent useCase Node attached to this Document node
     *
     * @return The parent UseCase node
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     * @throws Neo4jBadNodeFormatException
     */
    public UseCaseNode getParentUseCase()
            throws Neo4jNoResult, Neo4jBadNodeFormatException, Neo4jQueryException {
        RelationshipType relName = RelationshipType.withName(USECASE_TO_TAG_REL);

        Node n = getNode();
        Relationship parentRel = n.getSingleRelationship(relName, Direction.INCOMING);

        if (parentRel != null) {
            Node useCase = parentRel.getStartNode();
            return UseCaseNode.fromNode(this.neo4jAL, useCase);
        } else {
            return null;
        }
    }

    /**
     * Execute the Document node and create the associated document on imaging
     *
     * @param applicationLabel
     * @return <code>List<Node></code> the list of node concerned by the document creation
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public List<Node> execute(String applicationLabel)
            throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        if (this.getNode() == null)
            throw new Neo4jBadRequestException(
                    "Cannot execute this action. Associated node does not exist.", ERROR_PREFIX + "EXEC1");

        String forgedReq = null;

        try {
            forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.processReturn(forgedReq);
            forgedReq = TagProcessing.removeRemainingAnchors(forgedReq);

            Result res = neo4jAL.executeQuery(forgedReq);

            List<Node> toDocNode = new ArrayList<>();
            while (res.hasNext()) {
                try {
                    Node n = (Node) res.next().get(RETURN_ANCHOR);
                    toDocNode.add(n);
                } catch (Exception ignored) {
                    // Ignored
                }
            }

            DocumentItGenerator.create(neo4jAL, applicationLabel, title, description, toDocNode);
            return toDocNode;

        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            neo4jAL.logError("Cannot execute : " + forgedReq, e);
            throw new Neo4jBadRequestException(
                    "The request failed to execute.", this.request, e, ERROR_PREFIX + "EXEC2");
        }
    }

    /**
     * Launch the request against an application, without tagging the results
     *
     * @param applicationLabel Label of the application
     * @return The Number of node concerned by the document
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public Long forecastRequest(String applicationLabel)
            throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        if (this.getNode() == null)
            throw new Neo4jBadRequestException(
                    "Cannot execute this action. Associated node does not exist.", ERROR_PREFIX + "EXEC1");

        try {
            String forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.forgeCountRequest(forgedReq);

            Result res = neo4jAL.executeQuery(forgedReq);

            Long numAffected = 0L;
            if (res.hasNext()) {
                numAffected = (Long) res.next().get(COUNT_RETURN_VAL);
            }

            return numAffected;

        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            throw new Neo4jBadRequestException(
                    "The request failed to execute.", this.request, e, ERROR_PREFIX + "EXEC2");
        }
    }
}
