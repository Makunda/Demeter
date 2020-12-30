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
import com.castsoftware.demeter.models.demeter.SaveNode;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class BackupNode extends Neo4jObject{


    // Static Properties
    private static final String LABEL = Configuration.get("demeter.backup.node.label");
    private static final String NODE_GEN_REQUEST_PROPERTY = Configuration.get("demeter.backup.node.node_gen_request");
    private static final String NODE_LABEL_PROPERTY = Configuration.get("demeter.backup.node.node_label");

    private static final String GEN_REQUEST_RETURN_VALUE= Configuration.get("demeter.backup.node.node_gen_request.return_val");

    private static final String BACKUP_COPY_RELATION = Configuration.get("demeter.backup.relationship.copy");
    private static final String BACKUP_NODES_RELATION = Configuration.get("demeter.backup.relationship.type");
    private static final String BACKUP_NODES_OLD_REL_NAME = Configuration.get("demeter.backup.relationship.old_relation_name");



    private static final String ERROR_PREFIX = "BAKNx";

    // Properties
    private Neo4jAL neo4jAL;
    private String nodeGenReq;
    private String nodeLabel;

    // Static getters
    public static String getLabel() {
        return LABEL;
    }
    public static String getNodeGenRequestProperty() {
        return NODE_GEN_REQUEST_PROPERTY;
    }
    public static String getNodeLabelProperty() {
        return  NODE_LABEL_PROPERTY;
    }

    // Getters
    public String getNodeGenReq() {
        return nodeGenReq;
    }
    public String getNodeLabel() {
        return nodeLabel;
    };

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
            String nodeLabel = (String) node.getProperty(getNodeLabelProperty());

            BackupNode backupNode = new BackupNode(neo4jAL, nodeGenReq, nodeLabel);
            backupNode.setNode(node);

            return backupNode;
        } catch (NotFoundException | NullPointerException | ClassCastException e) {
            throw new Neo4jBadNodeFormatException(LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
        }
    }


    @Override
    public Node createNode() throws Neo4jNoResult {
        // Merge existing node based on their GenerationRequest
        try {
            Transaction tx = neo4jAL.getTransaction(); // Get actual transaction

            Node n = tx.createNode(Label.label(LABEL)); // Create node & add label
            n.setProperty(getNodeGenRequestProperty(), getNodeGenReq());
            n.setProperty(getNodeLabelProperty(), getNodeLabel());

            this.setNode(n);
            return n;
        } catch (NoSuchElementException |
                NullPointerException e) {
            throw new Neo4jNoResult(LABEL + "node creation failed",  "", e, ERROR_PREFIX+"CRN2");
        }
    }

    /**
     * Generate the node associated with the request. If this node have backup parents node, they will also be generated and linked to the newly created node.
     * The backup node is consumed in the operation
     * @return The node created by this backup node
     */
    public Node startBackup() throws Neo4jQueryException, Neo4jBadRequestException, Neo4jNoResult, Neo4jBadNodeFormatException {
        RelationshipType copyRelationship = RelationshipType.withName(BACKUP_COPY_RELATION);
        Node backupNode = getNode();

        // Check if the backup node copies another node. If so, the node used will be the original one. Otherwise the node is recreated from the backup request
        Node referenceNode = null;
        Iterator<Relationship> relIt = backupNode.getRelationships(Direction.OUTGOING, copyRelationship).iterator();
        if(relIt.hasNext()) {
            // Get existing node being copy by the backup
            referenceNode = relIt.next().getEndNode();
        } else {
            // Get the node returned by the query
            try {
                // Generate the node via the nodeGenRequest
                String req = getNodeGenReq().replaceAll("\\\\'", "'");
                neo4jAL.logInfo("Request to be executed : " + req);
                Result res = neo4jAL.executeQuery(req);

                // Throw error if no result returned by the query
                if(!res.hasNext()) {
                    throw new Neo4jNoResult("The request supposed to generate the node produced no result.", getNodeGenReq(), ERROR_PREFIX+"GENB1");
                }

                referenceNode = (Node) res.next().get(GEN_REQUEST_RETURN_VALUE);
            } catch (NullPointerException e) {
                throw new Neo4jBadRequestException("The request supposed to generate does not return result labeled correctly'.", ERROR_PREFIX+"GENB2");
            }
        }

        // Get attached relationships
        for(Relationship relation : backupNode.getRelationships(RelationshipType.withName(BACKUP_NODES_RELATION))) {
            String toCreateRelName = null;
            // Get name of the relationship to create.
            try {
                toCreateRelName = (String) relation.getProperty(BACKUP_NODES_OLD_REL_NAME);
            } catch (NullPointerException e) {
                String message = String.format("The relationship with label '%s' between nodes with id %d and %d is malformed. Missing name value", relation.getType().name(), backupNode.getId(), referenceNode.getId());
                throw new Neo4jBadNodeFormatException(message, ERROR_PREFIX+"GENB3");
            }

            String mergeRel;
            if(backupNode.getId() == relation.getStartNodeId()) { // Merge outgoing
                Node endNode = relation.getEndNode();
                mergeRel = String.format("MATCH (n),(m) WHERE ID(n)=%d AND ID(m)=%d MERGE (n)-[r:%s]->(m) RETURN r as rel;", referenceNode.getId(), endNode.getId(), toCreateRelName);
            } else {  // Merge incoming
                Node endNode = relation.getStartNode();
                mergeRel = String.format("MATCH (n),(m) WHERE ID(n)=%d AND ID(m)=%d MERGE (n)-[r:%s]->(m) RETURN r as rel;", endNode.getId(), referenceNode.getId(), toCreateRelName);
            }

            // Execute the relationship
            neo4jAL.executeQuery(mergeRel);
            relation.delete();
        }

        // Detach Delete backup node once the operation is done
        for(Relationship rel : backupNode.getRelationships()) {
            rel.delete();
        }
        backupNode.delete();

        return  referenceNode;
    }

    /**
     * Create a backup node and backup provided node's relationships too.
     * @param neo4jAL Neo4j Access Layer
     * @param applicationContext Application concerned by the backup
     * @param node Node to backup
     * @param backupRequest Associated backup request ( the request will be executed to recreate the node)
     * @param nodesAffected Potentials nodes affected by this change, to avoid a full backup.
     * @return The backup node created
     */
    public static Node createBackup(Neo4jAL neo4jAL, String applicationContext, Node node, String backupRequest, List<Node> nodesAffected) throws Neo4jNoResult, Neo4jQueryException {
        RelationshipType backupRelName = RelationshipType.withName(BACKUP_NODES_RELATION);
        RelationshipType copyRelName = RelationshipType.withName(BACKUP_COPY_RELATION);
        List<Long> affectedIds = nodesAffected.stream().map(Node::getId).collect(Collectors.toList());

        List<String> labelList = new ArrayList<>();
        // Retrieve node label
        for(Label l : node.getLabels()) {
            // Ignore application label
            if(l.name().equals(applicationContext)) continue;
            labelList.add(l.name());
        }

        // Merge backup  node with associated request
        BackupNode bkn = new BackupNode(neo4jAL, backupRequest, String.join(":", labelList));
        Node bkNode = bkn.createNode(); // Create node
        bkNode.addLabel(Label.label(applicationContext)); // Application context to the node as a label

        int numCreatedRel = 0;

        // Create a Copy Link Relationship to the  actual node. The copy link will be used if the node wasn't deleted
        bkNode.createRelationshipTo(node, copyRelName);

        // Merge backup links
        for(Relationship rel : node.getRelationships()) {
            if(rel.isType(backupRelName) || rel.isType(copyRelName)) continue;

            String relTypeString = rel.getType().toString();
            Node otherNode = rel.getOtherNode(node);

            // Check if the node id is part of potential candidates id.
            if(!affectedIds.contains(otherNode.getId())) continue; // Ignore node not part of potential affected candidates

            // Create relationship between node and newly created backup node
            Relationship createdRel = null;
            String mergeRel = null;
            if(rel.getStartNodeId() == node.getId()) { // Outgoing rel
                mergeRel = String.format("MATCH (n),(m) WHERE ID(n)=%d AND ID(m)=%d MERGE (n)-[r:%s]->(m) RETURN r as rel;", bkNode.getId(), otherNode.getId(), BACKUP_NODES_RELATION);
            } else { //  Incoming rel
                mergeRel = String.format("MATCH (n),(m) WHERE ID(n)=%d AND ID(m)=%d MERGE (m)-[r:%s]->(n) RETURN r as rel;", bkNode.getId(), otherNode.getId(), BACKUP_NODES_RELATION);
            }

            // Execute merge and get rel
            Result res = neo4jAL.executeQuery(mergeRel);
            if(!res.hasNext()) { // Is no result throw an error
                throw new Neo4jNoResult("The request creating relationship produced no result.", mergeRel, ERROR_PREFIX+"CREA1");
            }

            // Add relationship properties
            createdRel = (Relationship) res.next().get("rel");
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
    public static List<BackupNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jNoResult {
        Label label = Label.label(LABEL);
        List<BackupNode> returnList = new ArrayList<>();

        Node n = null;
        for (ResourceIterator<Node> it = neo4jAL.getTransaction().findNodes(label); it.hasNext(); ) {
            try {
                returnList.add(fromNode(neo4jAL, it.next()));
            }  catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
                throw new Neo4jNoResult(LABEL + "nodes retrieving by application name failed",  "findQuery", e, ERROR_PREFIX+"GANA1");
            }

        }

        return returnList;
    }

    /**
     * Get all backup node for a specific application and of a specific label
     * @param neo4jAL Neo4j Access Layer
     * @param applicationContext Application concerned by the backup
     * @param nodeLabel Label to search in backup
     * @return The list of Backup node matching the request
     * @throws Neo4jBadRequestException If the request to retrieve the nodes failed
     * @throws Neo4jBadNodeFormatException  If backup nodes aren't in the correct format
     */
    public static List<BackupNode> getApplicationBackupNode(Neo4jAL neo4jAL, String applicationContext, String nodeLabel) throws Neo4jBadRequestException, Neo4jBadNodeFormatException {
        String forgedLabel = LABEL + ":" + applicationContext;
        String bkAppQuery = String.format("MATCH (p:%s) WHERE p.%s='%s' RETURN p as node;",
                forgedLabel, BackupNode.getNodeLabelProperty(), nodeLabel);
        List<BackupNode> resNodes = new ArrayList<>();
        try {
            Result res = neo4jAL.executeQuery(bkAppQuery);
            while(res.hasNext()) {
                Node bkn = (Node) res.next().get("node");
                resNodes.add(BackupNode.fromNode(neo4jAL, bkn));
            }
        } catch (Neo4jQueryException e) {
            throw new Neo4jBadRequestException(LABEL + " nodes retrieving by label failed", bkAppQuery , e, ERROR_PREFIX+"DEL1");
        }

        return resNodes;
    }


    @Override
    public int hashCode() {
        return Objects.hash(nodeGenReq);
    }

    public BackupNode(Neo4jAL neo4jAL, String nodeGenReq, String nodeLabel) {
        super(neo4jAL);
        this.neo4jAL = neo4jAL;
        this.nodeGenReq = nodeGenReq;
        this.nodeLabel = nodeLabel;
    }
}
