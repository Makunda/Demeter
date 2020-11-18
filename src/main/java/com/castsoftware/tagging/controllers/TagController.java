package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.Neo4jObject;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.models.UseCaseNode;
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
    public static Node addTagNode(Neo4jAL neo4jAL, String tag, Boolean active, String request, String description,  Long parentId) throws Neo4jQueryException, Neo4jBadRequestException, Neo4jNoResult {
        Node parent = neo4jAL.getNodeById(parentId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(useCaseLabel)) {
            throw new Neo4jBadRequestException(String.format("Can only attach a %s node to a %s node.", TagNode.getLabel() ,UseCaseNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        TagNode tagNode = new TagNode(neo4jAL, tag, active, request, description);
        Node n = tagNode.createNode();

        // Create the relation from the use case to the tag
        parent.createRelationshipTo(n, RelationshipType.withName(USE_CASE_TO_TAG_RELATIONSHIP));

        return n;
    }



}
