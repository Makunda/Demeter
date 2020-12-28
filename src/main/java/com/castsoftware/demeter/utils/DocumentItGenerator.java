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

package com.castsoftware.demeter.utils;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DocumentItGenerator {

    private static final String DOCUMENTIT_LABEL = Configuration.get("imaging.node.document_it.label");


    public static Node create(Neo4jAL nal, String applicationLabel, String title, String description, List<Node> toInclude, String[] tags) {
        // Create documentIT node
        Transaction tx = nal.getTransaction();
        Node n = tx.createNode(Label.label(DOCUMENTIT_LABEL));
        n.addLabel(Label.label(applicationLabel));

        // Add properties to the node
        n.setProperty("Id", UUID.randomUUID().toString());
        n.setProperty("Title", title);
        n.setProperty("Description", description);
        n.setProperty("ViewType", "Object");

        // Add tags if presents
        n.setProperty("Tags", tags);
        n.setProperty("ViewName", "");

        // Add object's AIP node & Link the objects
        ArrayList<String> nodeAipIdList = new ArrayList<>();
        for(Node o : toInclude) {
            try {
                String aipId = (String) o.getProperty("AipId");
                nodeAipIdList.add(aipId);
                o.createRelationshipTo(n, RelationshipType.withName("ContainsDocument"));
            } catch (ClassCastException e) {
                nal.logError("Cannot attach node with Id " + o.getId() + "to document.", e);
            }
        }

        String[] nodes = new String[nodeAipIdList.size()];
        nodes = nodeAipIdList.toArray(nodes);

        n.setProperty("Nodes", nodes);

        return n;
    }

    /**
     * Create a new document following CAST Imaging document structure
     * @param nal Neo4j Access layer
     * @param title Title of the document
     * @param description Description of the document
     * @param toInclude List of nodes to include in the document=
     * @return Document node created
     */
    public static Node create(Neo4jAL nal, String applicationLabel, String title, String description, List<Node> toInclude) {
        return DocumentItGenerator.create(nal, applicationLabel, title, description, toInclude, new String[0] );
    }
}
