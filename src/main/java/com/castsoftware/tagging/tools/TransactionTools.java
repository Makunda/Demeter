package com.castsoftware.tagging.tools;

import com.castsoftware.tagging.config.Configuration;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransactionTools {

    private static String TRANSACTION_OBJECT_REL = Configuration.get("imaging.relationship.transaction.object.description");
    private static String TRANSACTION_OBJECT_LABEL = Configuration.get("imaging.node.transaction_node.label");

    public static int getTransactionDepth(Node transaction) {
        List<Node> l = new ArrayList<>();
        Iterator<Relationship> relationships = transaction.getRelationships(Direction.OUTGOING, RelationshipType.withName(TRANSACTION_OBJECT_REL)).iterator();



        Node n = null;
        if(relationships.hasNext()) {
            n = relationships.next().getEndNode();
        }

        return 0;
    }
}

