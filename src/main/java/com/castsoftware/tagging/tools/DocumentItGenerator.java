package com.castsoftware.tagging.tools;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DocumentItGenerator {

    private static final String DOCUMENTIT_LABEL = Configuration.get("imaging.node.document_it.label");

    public static Node create(Neo4jAL nal, String title, String description, List<Node> toInclude, Optional<List<String>> tags) {
        // Create documentIT node
        Transaction tx = nal.getTransaction();
        Node n = tx.createNode(Label.label(DOCUMENTIT_LABEL));

        // Add properties to the node
        n.setProperty("Id", UUID.randomUUID());
        n.setProperty("Title", title);
        n.setProperty("Description", description);
        n.setProperty("ViewType", "Object");

        // Add tags if presents
        if(tags.isPresent()) {
            String tagsAsString = String.format("[%s]", String.join(",", tags.get()));
            n.setProperty("Tags", tagsAsString);
        }else {
            n.setProperty("Tags", "[]");
        }
        n.setProperty("ViewName", "");

        // Add object's AIP node & Link the objects
        ArrayList<String> nodeAipIdList = new ArrayList<>();
        for(Node o : toInclude) {
            try {
                String aipId = String.valueOf((Long) o.getProperty("AipId"));
                nodeAipIdList.add(aipId);
                o.createRelationshipTo(n, RelationshipType.withName("ContainsDocument"));
            } catch (ClassCastException ignoredMe) {
            }
        }

        n.setProperty("Node", String.join(",", nodeAipIdList));

        return n;
    }
}
