package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.models.UseCaseNode;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import java.util.ArrayList;
import java.util.List;

public class TagController {

    private static final String ERROR_PREFIX = "TAGCx";
    private static final String USE_CASE_TO_TAG_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_tag");

    /**
     * Add a Tag Node and link it to a Use Case node.
     * @param neo4jAL Neo4j Access Layer
     * @param tag Tag that will be applied on nodes matching the request
     * @param active Status of activation. If this parameter is equal to "false" the request will be ignored.
     * @param request Request matching the nodes to be tag
     * @param parentId Id of the parent use case
     * @return <code>Node</code> the node created
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequest
     * @throws Neo4jNoResult
     */
    public static Node addTagNode(Neo4jAL neo4jAL, String tag, Boolean active, String request, Long parentId) throws Neo4jQueryException, Neo4jBadRequest, Neo4jNoResult {
        Node parent = neo4jAL.getNodeById(parentId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(useCaseLabel)) {
            List<String> l = new ArrayList<>();

            for(Label lab : parent.getLabels()) {
                l.add(lab.name());
            }

            neo4jAL.info("Parents Id labels = " + String.join(" ", l));
            throw new Neo4jBadRequest(String.format("Can only attach a %s node to a %s node.", TagNode.getLabel() ,UseCaseNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        TagNode tagNode = new TagNode(neo4jAL, tag, active, request);
        Node n = tagNode.createNode();

        // Create the relation from the use case to the tag
        parent.createRelationshipTo(n, RelationshipType.withName(USE_CASE_TO_TAG_RELATIONSHIP));

        return n;
    }

}
