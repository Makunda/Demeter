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

package com.castsoftware.demeter.models;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class BackupNode extends Neo4jObject{


    // Static Properties
    private static final String LABEL = Configuration.get("demeter.backup.node.label");
    private static final String NODE_GEN_REQUEST_PROPERTY = Configuration.get("demeter.backup.node.node_gen_request");
    private static final String GEN_REQUEST_RETURN_VALUE= Configuration.get("demeter.backup.node.node_gen_request.return_val");

    private static final String BACKUP_NODES_RELATION = Configuration.get("demeter.backup.relationship.type");
    private static final String BACKUP_NODES_OLD_REL_NAME = Configuration.get("demeter.backup.relationship.old_relation_name");



    private static final String ERROR_PREFIX = "BAKNx";

    // Properties
    private Neo4jAL neo4jAL;
    private String nodeGenReq;


    // Static getters
    public static String getLabel() {
        return LABEL;
    }
    public static String getNodeGenRequestProperty() {
        return NODE_GEN_REQUEST_PROPERTY;
    }

    // Getters
    public String getNodeGenReq() {
        return nodeGenReq;
    }

    /**
     * Create a Level5 Node object from a neo4j node
     * @param neo4jAL Neo4j Access Layer
     * @param node Node associated to the object
     * @return <code>ConfigurationNode</code> the object associated to the node.
     * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or malformed property.
     */
    public static BackupNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {
        if(!node.hasLabel(Label.label(LABEL))) {
            throw new Neo4jBadNodeFormatException(String.format("The node with Id '%d' does not contain the correct label. Expected to have : %s", node.getId(), LABEL), ERROR_PREFIX + "FROMN1");
        }

        try {
            // Initialize the node
            String nodeGenReq = (String) node.getProperty(getNodeGenRequestProperty());

            BackupNode backupNode = new BackupNode(neo4jAL, nodeGenReq);
            backupNode.setNode(node);

            return backupNode;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            throw new Neo4jBadNodeFormatException(LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }

    @Override
    protected Node findNode() throws Neo4jBadRequestException, Neo4jNoResult {
        String initQuery = String.format("MATCH (n:%s) WHERE ID(n)=%d RETURN n as node LIMIT 1;", LABEL, this.getNodeId());
        try {
            Result res = neo4jAL.executeQuery(initQuery);
            Node n = (Node) res.next().get("node");
            this.setNode(n);

            return n;
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node initialization failed", initQuery , e, ERROR_PREFIX+"FIN1");
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(String.format("You need to create %s node first.", LABEL),  initQuery, e, ERROR_PREFIX+"FIN2");
        }
    }

    @Override
    public Node createNode() throws Neo4jNoResult {
        // Merge existing node based on their GenerationRequest
        try {
            Transaction tx = neo4jAL.getTransaction();
            String forgedMergeReq = String.format("MERGE (o:%s { %s: '%s' }) RETURN o as node;", LABEL, getNodeGenRequestProperty(), getNodeGenReq());

            Result res = tx.execute(forgedMergeReq);

            Node n = (Node) res.next().get("node");

            this.setNode(n);
            return n;
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  "", e, ERROR_PREFIX+"CRN2");
        }
    }

    /**
     * Generate the node associated with the request. If this node have backup parents node, they will also be generated and linked to the newly created node.
     * @return The node created by this backup node
     */
    public Node startBackup() throws Neo4jQueryException, Neo4jBadRequestException, Neo4jNoResult, Neo4jBadNodeFormatException {
        Node backupNode = getNode();

        // Generate the node
        Result res = neo4jAL.executeQuery(getNodeGenReq());

        // Throw error if no result returned by the query
        if(!res.hasNext()) {
            throw new Neo4jNoResult("The request supposed to generate the node produced no result.", getNodeGenReq(), ERROR_PREFIX+"GENB1");
        }

        // Get the node returned by the query
        Node createdNode = null;
        try {
            createdNode = (Node) res.next().get(GEN_REQUEST_RETURN_VALUE);
        } catch (NullPointerException e) {
            throw new Neo4jBadRequestException("The request supposed to generate does not return result labeled correctly'.", ERROR_PREFIX+"GENB2");
        }

        // Get attached relationships
        for(Relationship incomingRel : backupNode.getRelationships(Direction.INCOMING, RelationshipType.withName(BACKUP_NODES_RELATION))) {
            Node parentBackup = incomingRel.getStartNode();
            String toCreateRelName = null;

            // Get name of the relationship to create.
            try {
                toCreateRelName = (String) incomingRel.getProperty(BACKUP_NODES_OLD_REL_NAME);
            } catch (NullPointerException e) {
                String message = String.format("The relationship with label '%s' between nodes with id %d and %d is malformed. Missing name value", incomingRel.getType().name(), backupNode.getId(), parentBackup.getId());
                throw new Neo4jBadNodeFormatException(message, ERROR_PREFIX+"GENB3");
            }

            Node toLink = parentBackup;
            // If parent node is also a backup, instantiate it and get its node
            if(parentBackup.hasLabel(BackupNode::getLabel)) {
                BackupNode bkn = BackupNode.fromNode(neo4jAL, parentBackup);
                toLink = bkn.startBackup();
            }

            // Merge the relation between parent node and newly created node
            // Search for existing rel with same type
            boolean exist = false;
            RelationshipType toCreateRelType = RelationshipType.withName(toCreateRelName);
            for(Relationship r : toLink.getRelationships(Direction.OUTGOING, toCreateRelType)) {
                if(r.getEndNodeId() == createdNode.getId() &&  toCreateRelType == r.getType()) {
                    exist = true;
                }
            }

            // Create if not exist
            if(!exist) {
                toLink.createRelationshipTo(createdNode, toCreateRelType);
            }

        }

        return  backupNode;
    }

    /**
     * Create a backup node and backup provided node's relationships too.
     * @param neo4jAL Neo4j Access Layer
     * @param node Node to backup
     * @param backupRequest Associated backup request ( the request will be executed to recreate the node)
     * @return The backup node created
     */
    public static Node createBackup(Neo4jAL neo4jAL, Node node, String backupRequest) throws Neo4jNoResult {
        RelationshipType backupRelName = RelationshipType.withName(BACKUP_NODES_RELATION);

        // Create backup  node with associated request
        BackupNode bkn = new BackupNode(neo4jAL, backupRequest);
        Node bkNode = bkn.createNode();

        // Create backup links
        for(Relationship rel : node.getRelationships()) {
            if(rel.isType(backupRelName)) continue;

            String relTypeString = rel.getType().toString();
            Node otherNode = rel.getOtherNode(node);

            // Create relationship between node and newly created backup node
            Relationship createdRel = null;
            if(rel.getStartNodeId() == node.getId()) { // Outgoing rel
                createdRel = bkNode.createRelationshipTo(otherNode, RelationshipType.withName(BACKUP_NODES_RELATION));
            } else { //  Incoming rel
                createdRel = otherNode.createRelationshipTo(bkNode, RelationshipType.withName(BACKUP_NODES_RELATION));
            }

            // Add relationship properties
            createdRel.setProperty(BACKUP_NODES_OLD_REL_NAME, relTypeString);

            // TODO : Handle other properties for the relationship

        }

        return bkNode;

    }


    /**
     * Return all Level5Node node in the database
     * @param neo4jAL Neo4j Access Layer
     * @return The list of node found in the database
     * @throws Neo4jBadRequestException If the request failed to execute
     */
    public static List<BackupNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jBadRequestException {
        try {
            List<BackupNode> resList = new ArrayList<>();
            ResourceIterator<Node> resIt = neo4jAL.findNodes(Label.label(LABEL));
            while ( resIt.hasNext() ) {
                try {
                    Node node = (Node) resIt.next();

                    BackupNode trn = BackupNode.fromNode(neo4jAL, node);
                    trn.setNode(node);

                    resList.add(trn);
                }  catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                    throw new Neo4jNoResult(LABEL + " nodes retrieving failed",  "findQuery", e, ERROR_PREFIX+"GAN1");
                }
            }
            return resList;
        } catch (Neo4jQueryException | Neo4jNoResult e) {
            throw new Neo4jBadRequestException(LABEL + " nodes retrieving failed", "findQuery" , e, ERROR_PREFIX+"GAN1");
        }
    }

    @Override
    public void deleteNode() throws Neo4jBadRequestException {
        String queryDomain = String.format("MATCH (p:%s) WHERE ID(p)=%d DETACH DELETE p;",
                LABEL, this.getNodeId());
        try {
            neo4jAL.executeQuery(queryDomain);
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " node deletion failed", queryDomain , e, ERROR_PREFIX+"DEL1");
        }
    }

    public BackupNode(Neo4jAL neo4jAL, String nodeGenReq) {
        super(neo4jAL);
        this.neo4jAL = neo4jAL;
        this.nodeGenReq = nodeGenReq;
    }
}
