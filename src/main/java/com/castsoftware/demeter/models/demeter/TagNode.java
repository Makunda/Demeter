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
import org.neo4j.graphdb.*;

import java.util.*;

public class TagNode extends Neo4jObject {

    // Configuration properties
    private static final String LABEL = Configuration.get("neo4j.nodes.t_tag_node");
    private static final String TAG_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.tag");
    private static final String REQUEST_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.request");
    private static final String ACTIVE_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.active");
    private static final String DESCRIPTION_PROPERTY = Configuration.get("neo4j.nodes.t_use_case.description");
    private static final String CATEGORIES_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.categories");
    private static final String IGNORE_PREFIX_PROPERTY = Configuration.get("neo4j.nodes.t_tag_node.ignore_prefix");
    private static final String ERROR_PREFIX = Configuration.get("neo4j.nodes.t_tag_node.error_prefix");
    private static final String USECASE_TO_TAG_REL = Configuration.get("neo4j.relationships.use_case.to_tag");

    private static final String LABEL_ANCHOR = Configuration.get("tag.anchors.label");
    private static final String COUNT_RETURN_VAL = Configuration.get("tag.anchors.countReturn.return_val");
    private static final String RETURN_ANCHOR = Configuration.get("tag.anchors.return.return_val");

    private static final String TAG_PREFIX = Configuration.get("demeter.prefix.tags");


    // Node properties
    private String tag;
    private String request;
    private Boolean active;
    private String description;
    private String categories;
    private Boolean ignorePrefix;

    public TagNode(Neo4jAL nal, String tag, Boolean active, String request, String description) {
        super(nal);
        this.tag = tag;
        this.active = active;
        this.request = request;
        this.description = description;
        this.ignorePrefix = false;
    }

    public TagNode(Neo4jAL nal, String tag, Boolean active, String request, String description, Boolean ignorePrefix) {
        super(nal);
        this.tag = tag;
        this.active = active;
        this.request = request;
        this.description = description;
        this.ignorePrefix = ignorePrefix;
    }

    public static String getLabel() {
        return LABEL;
    }

    public static String getTagProperty() {
        return TAG_PROPERTY;
    }

    public static String getActiveProperty() {
        return ACTIVE_PROPERTY;
    }

    public static String getRequestProperty() {
        return REQUEST_PROPERTY;
    }

    public static String getIgnorePrefixProperty() {
        return IGNORE_PREFIX_PROPERTY;
    }

    public static String getCategoriesProperty() {
        return CATEGORIES_PROPERTY;
    }

    /**
     * Create a TagRequestNode Node object from a neo4j node
     *
     * @param neo4jAL Neo4j Access Layer
     * @param node    Node associated to the object
     * @return <code>TagRequestNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static TagNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {

        if (!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException(String.format("The node with Id '%d' does not contain the correct label. Expected to have : %s", node.getId(), LABEL), ERROR_PREFIX + "FROMN1");
        }

        try {
            String tag = (String) node.getProperty(TAG_PROPERTY);

            boolean active = Neo4jObject.castPropertyToBoolean(node.getProperty(UseCaseNode.getActiveProperty()));
            String request = (String) node.getProperty(REQUEST_PROPERTY);

            String description = "";
            if (node.hasProperty(DESCRIPTION_PROPERTY))
                description = (String) node.getProperty(DESCRIPTION_PROPERTY);

            String categories = "";
            if (node.hasProperty(CATEGORIES_PROPERTY))
                categories = (String) node.getProperty(CATEGORIES_PROPERTY);

            Boolean ignorePrefix = false;
            if (node.hasProperty(IGNORE_PREFIX_PROPERTY)) {
                ignorePrefix = (Boolean) node.getProperty(IGNORE_PREFIX_PROPERTY);
            }

            // Initialize the node
            TagNode trn = new TagNode(neo4jAL, tag, active, request, description, ignorePrefix);
            trn.setCategories(categories);
            trn.setNode(node);

            return trn;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            throw new Neo4jBadNodeFormatException(LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }

    public static List<TagNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jNoResult {
        Label label = Label.label(LABEL);
        List<TagNode> returnList = new ArrayList<>();

        for (ResourceIterator<Node> it = neo4jAL.getTransaction().findNodes(label); it.hasNext(); ) {
            try {
                returnList.add(fromNode(neo4jAL, it.next()));
            } catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                throw new Neo4jNoResult(LABEL + "nodes retrieving by application name failed", "findQuery", e, ERROR_PREFIX + "GANA1");
            }

        }

        return returnList;
    }

    public String getTag() {
        return tag;
    }

    public String getRequest() {
        return request;
    }

    public Boolean getActive() {
        return active;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getIgnorePrefix() {
        return ignorePrefix;
    }

    @Override
    public Node createNode() throws Neo4jBadRequestException, Neo4jNoResult {
        String queryDomain = String.format("MERGE (p:%s { %s : \"%s\", %s : \"%s\", %s : %b, %s : \"%s\" }) RETURN p as node;",
                LABEL, TAG_PROPERTY, tag, REQUEST_PROPERTY, request, ACTIVE_PROPERTY, this.active, DESCRIPTION_PROPERTY, this.description);
        try {
            Result res = neo4jAL.executeQuery(queryDomain);
            Node n = (Node) res.next().get("node");
            this.setNode(n);
            return n;
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node creation failed", queryDomain, e, ERROR_PREFIX + "CRN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed", queryDomain, e, ERROR_PREFIX + "CRN2");
        }
    }

    /**
     * Execute the request of the tag in a specific context.
     *
     * @param applicationLabel The application that will be flagged by the request.
     * @return The list of tagged node
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public List<Node> executeRequest(String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        if (this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX + "EXEC1");

        String forgedTag;
        if (!ignorePrefix) {
            forgedTag = TAG_PREFIX + this.tag;
        } else {
            forgedTag = this.tag;
        }

        // Build parameters
        Map<String, Object> params = new HashMap<>();
        params.put("tagName", forgedTag);

        try {
            String forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.processAll(forgedReq);

            List<Node> nodeList = new ArrayList<>();
            Result res = neo4jAL.executeQuery(forgedReq, params);

            while (res.hasNext()) {
                Node n = (Node) res.next().get(RETURN_ANCHOR);
                nodeList.add(n);
            }

            return nodeList;
        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            throw new Neo4jBadRequestException("The request failed to execute.", this.request, e, ERROR_PREFIX + "EXEC2");
        }
    }

    /**
     * Launch the request against the Database, without tagging the results
     *
     * @param applicationLabel
     * @return
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public Long forecastRequest(String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        if (this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX + "EXEC1");

        // Build parameters
        Map<String, Object> params = new HashMap<>();
        params.put("tagName", this.tag);

        try {
            String forgedReq = TagProcessing.processApplicationContext(this.request, applicationLabel);
            forgedReq = TagProcessing.forgeCountRequest(forgedReq);

            Result res = neo4jAL.executeQuery(forgedReq, params);

            Long numAffected = 0L;
            if (res.hasNext()) {
                numAffected = (Long) res.next().get(COUNT_RETURN_VAL);
            }

            return numAffected;

        } catch (Neo4jQueryException | NullPointerException | Neo4JTemplateLanguageException e) {
            throw new Neo4jBadRequestException("The request failed to execute.", this.request, e, ERROR_PREFIX + "EXEC2");
        }
    }

    /**
     * Get the parent use case attached to this TagNode
     *
     * @return
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     * @throws Neo4jBadNodeFormatException
     */
    public UseCaseNode getParentUseCase() throws Neo4jNoResult, Neo4jBadNodeFormatException, Neo4jQueryException {
        RelationshipType relName = RelationshipType.withName(USECASE_TO_TAG_REL);

        Node n = getNode();
        Iterator<Relationship> relIt = n.getRelationships(Direction.INCOMING, relName).iterator();

        assert relIt.hasNext() : "Use case node has no parent. Bad configuration formatting.";

        Node useCase = relIt.next().getStartNode();

        return UseCaseNode.fromNode(this.neo4jAL, useCase);
    }

    /**
     * Test a query using "EXPLAIN" keyword in Neo4j. This function do not execute the query, but will produce an error if it's incorrect.
     *
     * @param applicationLabel The application that will be flagged by the request.
     * @return <code>Boolean</code> True if the request is valid, false otherwise.
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public boolean checkQuery(String applicationLabel) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        if (this.getNode() == null)
            throw new Neo4jBadRequestException("Cannot execute this action. Associated node does not exist.", ERROR_PREFIX + "CHECK1");

        // Build parameters
        Map<String, Object> params = new HashMap<>();
        params.put("tagName", this.tag);

        // Forge the request, remove first and last quotes
        String req = this.request.replace(LABEL_ANCHOR, applicationLabel);
        String forgedReq = "EXPLAIN " + req.replaceAll("(^\\s\")|(\\s\"\\s?$)", "");

        try (Transaction tx = neo4jAL.getDb().beginTx()) {

            tx.execute(forgedReq, params);
            return true;
        } catch (QueryExecutionException e) {
            String m = String.format("The tag \"%s\" with associated request \"%s\" is not valid. Check Failed with error..", this.getTag(), this.getRequest());
            neo4jAL.getLogger().warn(m);
            return false;
        }

    }


}
