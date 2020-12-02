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

package com.castsoftware.demeter.controllers;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.DocumentNode;
import com.castsoftware.demeter.models.UseCaseNode;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DocumentController {

    private static final String ERROR_PREFIX = "DOCTGx";
    private static final String USECASE_TO_DOC_REL = Configuration.get("neo4j.relationships.use_case.to_document");

    /**
     * Add a DocumentNode and attach it to the specified useCase node.
     * On its execution, the node will create a Document It and link it to every node matching it.
     * @param nal Neo4j Access Layer
     * @param title Title of the document
     * @param request Request returning nodes matching the desired Use-Case
     * @param active Activation of the document
     * @param description Description of the Request, used to quickly understand the action of the request
     * @param documentDescription Description associated with the document
     * @param useCaseId Id of the parent Use Case
     * @return The document created
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     * @throws Neo4jQueryException
     */
    public static Node addDocumentNode(Neo4jAL nal, String title, String request, Boolean active, String description, String documentDescription, Long useCaseId) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        Node parent = nal.getNodeById(useCaseId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(useCaseLabel)) {
            throw new Neo4jBadRequestException(String.format("Can only attach a %s node to a %s.", DocumentNode.getLabel(), UseCaseNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        DocumentNode docNode = new DocumentNode(nal, title, request, active, description, documentDescription );
           Node n = docNode.createNode();

        // Create the relation to the use case
        parent.createRelationshipTo(n, RelationshipType.withName(USECASE_TO_DOC_REL));

        return n;
    }

    /**
     * Return all documents in an active branch, and flagged as active
     * @param neo4jAL Neo4j Access Layer
     * @param configurationName Name of the configuration to execute
     * @return The list of "ready to execute" nodes
     * @throws Neo4jBadRequestException
     * @throws Neo4jQueryException
     * @throws Neo4jNoResult
     */
    public static List<DocumentNode> getSelectedDocuments(Neo4jAL neo4jAL, String configurationName) throws Neo4jBadRequestException, Neo4jQueryException, Neo4jNoResult {

        Label documentLabel = Label.label(DocumentNode.getLabel());
        Set<Node> documents = UseCaseController.searchByLabelInActiveBranches(neo4jAL, configurationName, documentLabel);

        return documents.stream().map( x -> {
            try {
                return DocumentNode.fromNode(neo4jAL, x);
            } catch (Neo4jBadNodeFormatException ex) {
                neo4jAL.getLogger().error("Error during Tag Nodes discovery.", ex);
                return null;
            }
        }).filter(x -> x != null && x.getActive()).collect(Collectors.toList());
    }




}
