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
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.Neo4jObject;
import com.castsoftware.demeter.models.demeter.ConfigurationNode;
import com.castsoftware.demeter.models.demeter.UseCaseNode;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class UseCaseController {

    private static final String ERROR_PREFIX = "USECCx";
    private static final String USE_CASE_RELATIONSHIP =
            Configuration.get("neo4j.relationships.use_case.to_use_case");
    private static final String DEFAULT_SELECTED_VALUE =
            Configuration.get("neo4j.nodes.default.selected");

    /**
     * Add a use case node to a configuration or another use case node
     *
     * @param neo4jAL  Neo4J Access Layer
     * @param name     Name associated to the use case
     * @param active   Activation status of the usecase
     * @param parentId Id of the parent node
     * @return the node associated to the use case created
     * @throws Neo4jQueryException      An error happened during the execution of the query
     * @throws Neo4jBadRequestException The request didn't returned the expected results
     * @throws Neo4jNoResult            The request didn't return any result
     */
    public static Node addUseCase(Neo4jAL neo4jAL, String name, Boolean active, Long parentId)
            throws Neo4jQueryException, Neo4jBadRequestException, Neo4jNoResult {
        Node parent = neo4jAL.getNodeById(parentId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());
        Label configLabel = Label.label(ConfigurationNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if (!parent.hasLabel(useCaseLabel) && !parent.hasLabel(configLabel)) {
            throw new Neo4jBadRequestException(
                    String.format(
                            "Can only attach a %s node to a %s node or a %s node.",
                            UseCaseNode.getLabel(), UseCaseNode.getLabel(), ConfigurationNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        Boolean selected = Boolean.parseBoolean(DEFAULT_SELECTED_VALUE);
        UseCaseNode useCaseNode = new UseCaseNode(neo4jAL, name, active, selected);
        Node n = useCaseNode.createNode();

        // Create the relation to the use case
        parent.createRelationshipTo(n, RelationshipType.withName(USE_CASE_RELATIONSHIP));

        return n;
    }

    /**
     * Return all use case nodes present in the database
     *
     * @param neo4jAL Neo4J A
     * @return list of the use cases
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequestException
     */
    public static List<UseCaseNode> listUseCases(Neo4jAL neo4jAL)
            throws Neo4jQueryException, Neo4jBadRequestException {
        return UseCaseNode.getAllNodes(neo4jAL);
    }

    /**
     * Return all active use case nodes present in the database
     *
     * @param neo4jAL Neo4J A
     * @return list of active use cases in the database
     * @throws Neo4jQueryException
     * @throws Neo4jBadRequestException
     */
    public static List<UseCaseNode> listActiveUseCases(Neo4jAL neo4jAL)
            throws Neo4jQueryException, Neo4jBadRequestException {
        return UseCaseNode.getAllNodes(neo4jAL).stream()
                .filter(UseCaseNode::getActive)
                .collect(Collectors.toList());
    }

    /**
     * Set the activation value of all use cases in the configuration
     *
     * @param neo4jAL Neo4j Access layer
     * @param status  New value of hte activation parameter
     * @return <code>int</code> The number of node modified
     * @throws Neo4jQueryException
     */
    public static int activateAllUseCase(Neo4jAL neo4jAL, Boolean status) throws Neo4jQueryException {
        int changes = 0;

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());
        ResourceIterator<Node> useCases = neo4jAL.findNodes(useCaseLabel);

        while (useCases.hasNext()) {
            Node n = useCases.next();
            n.setProperty(UseCaseNode.getActiveProperty(), status);
            changes++;
        }

        return changes;
    }

    /**
     * Set the Selected value of all use cases in the configuration
     *
     * @param neo4jAL Neo4j Access layer
     * @param status  New value of hte activation parameter
     * @return <code>int</code> The number of node modified
     * @throws Neo4jQueryException
     */
    public static int selectAllUseCase(Neo4jAL neo4jAL, Boolean status) throws Neo4jQueryException {
        int changes = 0;

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());
        ResourceIterator<Node> useCases = neo4jAL.findNodes(useCaseLabel);

        while (useCases.hasNext()) {
            Node n = useCases.next();
            n.setProperty(UseCaseNode.getSelectedProperty(), status);
            changes++;
        }

        return changes;
    }

    /**
     * Change the status of the selected property in an use case node, and of every use case node
     * under it.
     *
     * @param neo4jAL Neo4j access layer
     * @param id      Id of the use case to modify
     * @return list of the use case modified during the action
     * @throws Neo4jQueryException
     */
    public static List<UseCaseNode> selectUseCase(Neo4jAL neo4jAL, Long id, Boolean status)
            throws Neo4jQueryException, Neo4jBadRequestException {

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        Node n = neo4jAL.getNodeById(id);

        // Check if the node provided if a Use Case node, otherwise throw an error
        if (!n.hasLabel(useCaseLabel))
            throw new Neo4jBadRequestException(
                    "Node does not contain the require label : " + UseCaseNode.getLabel(),
                    ERROR_PREFIX + "ACUSC1");

        Stack<Node> toVisit = new Stack<>();
        List<UseCaseNode> useCaseList = new ArrayList<>();

        toVisit.push(n);

        // Loop while node are discovered
        while (!toVisit.isEmpty()) {
            try {
                Node toTreat = toVisit.pop();
                toTreat.setProperty(UseCaseNode.getActiveProperty(), status);

                for (Relationship rel :
                        toTreat.getRelationships(
                                Direction.OUTGOING, RelationshipType.withName(USE_CASE_RELATIONSHIP))) {
                    Node otherNode = rel.getEndNode();

                    if (useCaseList.contains(otherNode) || !otherNode.hasLabel(useCaseLabel)) continue;

                    toVisit.add(otherNode);
                }

                useCaseList.add(UseCaseNode.fromNode(neo4jAL, toTreat));
            } catch (Neo4jBadNodeFormatException e) {
                neo4jAL.getLogger().warn("Failed to create object for UseCase node", e);
            }
        }

        return useCaseList;
    }

    /**
     * Search for nodes with a specific label inside the confirmation. The nodes with a matching label
     * and present in an active branch will be returned.
     *
     * @param neo4jAL           Neo4j Access Layer
     * @param configurationName Name of the configuration to parse
     * @param toFind            Label of node
     * @return The list of node that matched both conditions
     * @throws Neo4jBadRequestException
     * @throws Neo4jQueryException
     * @throws Neo4jNoResult
     */
    public static Set<Node> searchByLabelInActiveBranches(
            Neo4jAL neo4jAL, String configurationName, Label toFind)
            throws Neo4jBadRequestException, Neo4jQueryException, Neo4jNoResult {
        Label UseCaseLabel = Label.label(UseCaseNode.getLabel());
        Set<Node> matchingNodes = new HashSet<>();

        String req =
                String.format(
                        "MATCH(o:%s) WHERE o.%s=\"%s\" RETURN o as res",
                        ConfigurationNode.getLabel(), ConfigurationNode.getNameProperty(), configurationName);
        Result result = neo4jAL.executeQuery(req);

        if (!result.hasNext()) {
            throw new Neo4jNoResult(
                    String.format(
                            "The request to find Configuration node with name \"%s\" didn't produced any result.",
                            configurationName),
                    req,
                    ERROR_PREFIX + "GATG1");
        }

        Node confNode = null;

        try {
            confNode = (Node) result.next().get("res");
        } catch (NoSuchElementException | NullPointerException e) {
            throw new Neo4jBadRequestException(
                    "Error the request didn't return results in a correct format.", req, e, "GATG2");
        }

        // Iterate over Active Use Case
        Stack<Node> toVisit = new Stack<>();
        Set<Node> visited = new HashSet<>();

        // Start with the configuration node
        toVisit.add(confNode);

        while (!toVisit.isEmpty()) {
            Node n = toVisit.pop();

            // Check the activation value if useCase Node
            if (n.hasLabel(Label.label(UseCaseNode.getLabel()))) {
                // Check the value for active property
                boolean active =
                        Neo4jObject.castPropertyToBoolean(n.getProperty(UseCaseNode.getActiveProperty()));
                boolean selected =
                        Neo4jObject.castPropertyToBoolean(n.getProperty(UseCaseNode.getSelectedProperty()));

                if (!active || !selected) {
                    visited.add(n);
                    continue;
                }
            }

            // Check if UseCase Nodes are connected
            for (Relationship rel : n.getRelationships(Direction.OUTGOING)) {
                Node otherNode = rel.getEndNode();

                if (otherNode.hasLabel(UseCaseLabel) || !visited.contains(otherNode)) {
                    toVisit.add(otherNode);
                }

                if (otherNode.hasLabel(toFind)) {
                    matchingNodes.add(rel.getEndNode());
                }
            }

            visited.add(n);
        }

        // TagNode.fromNode(neo4jAL, otherNode)
        return matchingNodes;
    }
}
