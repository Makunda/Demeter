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
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.Neo4jObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Result;

import java.lang.reflect.Type;
import java.util.List;

public class OperationNode extends Neo4jObject {

    private static final String LABEL = Configuration.get("save.operation.node.name");
    private static final String GROUP_NAME_PROPERTY =
            Configuration.get("save.operation.node.groupName");
    private static final String TO_GROUP_PROPERTY = Configuration.get("save.operation.node.toGroup");

    private static final String OBJECT_TAG_PROPERTY =
            Configuration.get("imaging.link.object_property.tags");

    private static final String RELATION_TO_SAVE_NODE =
            Configuration.get("save.operation.node.links.to_save");
    private static final String ERROR_PREFIX = Configuration.get("save.operation.node.error_prefix");

    private final String groupName;
    private final List<String> toGroup;

    public OperationNode(Neo4jAL neo4jAL, String groupName, List<String> toGroup) {
        super(neo4jAL);
        this.groupName = groupName;
        this.toGroup = toGroup;
    }

    public static String getRelationToSaveNode() {
        return RELATION_TO_SAVE_NODE;
    }

    /**
     * Create Operation node from Neo4j node
     *
     * @param neo4jAL Neo4j Access Layer
     * @param node    Node to convert
     * @return the OperationNode created
     * @throws Neo4jBadNodeFormatException
     */
    public static OperationNode fromNode(Neo4jAL neo4jAL, Node node)
            throws Neo4jBadNodeFormatException {
        if (!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException(
                    String.format(
                            "The node with Id '%d' does not contain the correct label. Expected to have : %s",
                            node.getId(), LABEL),
                    ERROR_PREFIX + "FROMN1");
        }

        try {
            Type listType = new TypeToken<List<String>>() {
            }.getType();

            String groupName = (String) node.getProperty(GROUP_NAME_PROPERTY);

            String jsonString = (String) node.getProperty(TO_GROUP_PROPERTY);
            List<String> toGroup = new Gson().fromJson(jsonString, listType);

            // Initialize the node
            OperationNode opn = new OperationNode(neo4jAL, groupName, toGroup);
            opn.setNode(node);

            return opn;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            throw new Neo4jBadNodeFormatException(
                    LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }

    @Override
    public Node createNode() {
        Label label = Label.label(LABEL);

        // Serialize the list
        String toJsonList = new Gson().toJson(this.toGroup);

        Node n = neo4jAL.getTransaction().createNode(label);
        n.setProperty(GROUP_NAME_PROPERTY, this.groupName);
        n.setProperty(TO_GROUP_PROPERTY, toJsonList);

        this.setNode(n);
        return n;
    }

    /**
     * Re-execute the operations and apply group names
     *
     * @return Number of node grouped by this operation
     */
    public int execute(String applicationContext) throws Neo4jQueryException {
        int treatedNode = 0;

        String requestTemplate =
                "MATCH(n:"
                        + applicationContext
                        + ":Object) WHERE n.FullName='%s' SET n.Tags = CASE WHEN n.Tags IS NULL THEN ['"
                        + groupName
                        + "'] ELSE @.Tags + '"
                        + groupName
                        + "' END RETURN n as obj";

        String toExecute;
        Result res;
        for (String fullName : toGroup) {
            toExecute = String.format(requestTemplate, fullName);
            res = neo4jAL.executeQuery(toExecute);

            // Since fullName property isn't unique, iterate over objects
            while (res.hasNext()) {
                treatedNode++;
            }
        }

        return treatedNode;
    }
}
