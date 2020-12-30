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

package com.castsoftware.demeter.controllers.configuration;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.*;
import com.castsoftware.demeter.models.demeter.TagNode;
import com.castsoftware.demeter.models.demeter.UseCaseNode;
import com.castsoftware.demeter.results.demeter.TagResult;
import com.castsoftware.demeter.statistics.Highlights.Highlight;
import com.castsoftware.demeter.statistics.Highlights.HighlightType;
import com.castsoftware.demeter.tags.TagProcessing;
import com.castsoftware.exporter.exceptions.neo4j.Neo4jBadRequest;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class TagController {

    private static final String ERROR_PREFIX = "TAGCx";
    private static final String USE_CASE_TO_TAG_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_tag");
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");

    /**
     * Return all the activated node matching an "activated" use case route ( a path of use case, with the "Activate" parameter, set on "on")
     * @param neo4jAL Neo4j access layer
     * @param configurationName Name of the configuration to use
     * @return The list of activated tags
     * @throws Neo4jQueryException
     * @throws Neo4jNoResult
     * @throws Neo4jBadRequestException
     */
    public static List<TagNode> getSelectedTags(Neo4jAL neo4jAL, String configurationName) throws Neo4jQueryException, Neo4jNoResult, Neo4jBadRequestException {
        Label tagNodeLabel = Label.label(TagNode.getLabel());
        Set<Node> tags = UseCaseController.searchByLabelInActiveBranches(neo4jAL, configurationName, tagNodeLabel);

        //TagNode.fromNode(neo4jAL, otherNode)
        return tags.stream().map( x -> {
            try {
                return TagNode.fromNode(neo4jAL, x);
            } catch (Neo4jBadNodeFormatException ex) {
                neo4jAL.getLogger().error("Error during Tag Nodes discovery.", ex);
                return null;
            }
        }).filter(x -> x != null && x.getActive()).collect(Collectors.toList());
    }



    /**
     * Add a Tag Node and link it to a Use Case node.
     * @param neo4jAL Neo4j Access Layer
     * @param tag Tag that will be applied on nodes matching the request
     * @param active Status of activation. If this parameter is equal to "false" the request will be ignored.
     * @param request Request matching the nodes to be tag
     * @param parentId Id of the parent use case
     * @return <code>Node</code> the node created
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public static Node addTagNode(Neo4jAL neo4jAL, String tag, Boolean active, String request, String description, String categories, Long parentId) throws Neo4jQueryException, Neo4jBadRequestException, Neo4jNoResult {
        Node parent = neo4jAL.getNodeById(parentId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(useCaseLabel)) {
            throw new Neo4jBadRequestException(String.format("Can only attach a %s node to a %s node.", TagNode.getLabel() ,UseCaseNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        // Check the validity of the query
        if(!validateQuery(neo4jAL, request)) {
            throw new Neo4jBadRequestException(String.format("The request provided is in incorrect format. Request : '%s'", request), ERROR_PREFIX+"ADDU2");
        }


        TagNode tagNode = new TagNode(neo4jAL, tag, active, request, description);
        tagNode.setCategories(categories);
        Node n = tagNode.createNode();

        // Create the relation from the use case to the tag
        parent.createRelationshipTo(n, RelationshipType.withName(USE_CASE_TO_TAG_RELATIONSHIP));

        return n;
    }

    /**
     * Return the forecast of the tag on a specific application as a list of TagResult
     * @param neo4jAL Neo4j Access Layer
     * @param configurationName Name of the configuration to use
     * @param applicationName Name of the configuration
     * @return The list of TagResults
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     */
    public static List<TagResult> forecastTag(Neo4jAL neo4jAL, String configurationName, String applicationName) throws Neo4jQueryException, Neo4jBadRequestException, Neo4jNoResult {
        List<TagNode> tagNodeList = TagController.getSelectedTags(neo4jAL, configurationName);
        List<TagResult> tagResultList = new ArrayList<>();

        for(TagNode tn : tagNodeList) {
            try {
                // Ignored non active requests
                if(!tn.getActive()) continue;

                Long numAffected = tn.forecastRequest(applicationName);
                String useCaseName = tn.getParentUseCase().getName();
                TagResult tr = new TagResult(tn.getNodeId(), tn.getTag(), tn.getDescription(), numAffected, tn.getCategories(), useCaseName);
                tagResultList.add(tr);
            } catch (Exception | Neo4jNoResult | Neo4jBadNodeFormatException neo4jNoResult) {
                neo4jAL.logError(String.format("Tag with Id '%d' produced an error during forecasting.", tn.getNodeId()), neo4jNoResult);
            }
        }

        return tagResultList;
    }

    /**
     * Execute specified tag request
     * @param neo4jAL Neo4j Access Layer
     * @param id Id of the Tag Node
     * @param applicationContext Application target
     * @return Return Tag result
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequestException
     * @throws Neo4jBadNodeFormatException
     * @throws Neo4jNoResult
     */
    public static TagResult executeTag(Neo4jAL neo4jAL, Long id, String applicationContext) throws Neo4jQueryException, Neo4jBadRequestException, Neo4jBadNodeFormatException, Neo4jNoResult {
        Label tagLabel = Label.label(TagNode.getLabel());
        Node tagNode = neo4jAL.getNodeById(id);

        if(!tagNode.hasLabel(tagLabel))
            throw new Neo4jBadRequestException("The provided Id does not correspond to a Tag Node", ERROR_PREFIX+"EXET1");

        TagNode tn = TagNode.fromNode(neo4jAL, tagNode);

        int numAffected = tn.executeRequest(applicationContext).size();
        String useCaseName = tn.getParentUseCase().getName();
        return new TagResult(tn.getNodeId(), tn.getTag(), tn.getDescription(), (long) numAffected, tn.getCategories(), useCaseName);
    }

    /**
     * Check the validity of a query provided
     * @param neo4jAL Neo4j Access Layer
     * @param request The request to test
     * @return True is the test was a success, false otherwise
     */
    public static boolean validateQuery(Neo4jAL neo4jAL, String request) {
        // Check the validity og the provided request
        try {
            // Sanitization, keep, everything but semi-colons
            // TODO : Review the sanitization
            request = request.replaceAll(";", "");

            // Check the presence of anchors in the request
            if(!TagProcessing.isCountAnchorPresent(request) || ! TagProcessing.isReturnAnchorPresent(request)) {
                return false;
            }

            // Replace anchors and Execute explain
            String forgedReq = "EXPLAIN " + TagProcessing.processAll(request);
            forgedReq = TagProcessing.replaceDummyApplicationContext(forgedReq);
            neo4jAL.logInfo("Request to execute :" + forgedReq);
            Result result = neo4jAL.executeQuery(forgedReq);

            return true;

        } catch (Neo4jQueryException | Neo4JTemplateLanguageException e) {
            return false;
        }

    }

}
