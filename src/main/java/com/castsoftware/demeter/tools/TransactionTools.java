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

package com.castsoftware.demeter.tools;

import com.castsoftware.demeter.config.Configuration;
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

