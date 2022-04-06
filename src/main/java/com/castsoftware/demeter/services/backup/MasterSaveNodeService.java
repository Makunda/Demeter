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

package com.castsoftware.demeter.services.backup;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.backup.MasterSaveNode;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MasterSaveNodeService {


    private static final String TO_SAVE_NODE = "DECLARES";

    /**
     * Get the label of the node to save
     *
     * @return The label as string
     */
    public static String getLabelAsString() {
        return MasterSaveNode.MASTERSAVE_NODE_LABEL;
    }

    /**
     * Get the label of the node to save
     *
     * @return The label as label
     */
    public static Label getLabel() {
        return Label.label(MasterSaveNode.MASTERSAVE_NODE_LABEL);
    }

    /**
     * Get the relationship between master and save node
     *
     * @return The name of the relationship
     */
    public static String getRelationshipToSaveNode() {
        return TO_SAVE_NODE;
    }

    /**
     * Look for a save node in the application
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @param name        Name of the save
     * @return Optional of the node found
     */
    public static Optional<Node> findMasterSaveNodeByName(Neo4jAL neo4jAL, String application, String name) throws Exception {
        String request = String.format("MATCH (o:`%s`:`%s`) WHERE o.Name=$name RETURN o as node;", application, getLabelAsString());

        try {
            // Look for an existing object in the database
            Node n = null;
            Result res = neo4jAL.executeQuery(request, Map.of("name", name));
            if (res.hasNext()) return Optional.of((Node) res.next().get("node"));
            else return Optional.empty();

        } catch (Neo4jQueryException err) {
            neo4jAL.logError(String.format("Failed to get or create the save node. Request : '%s'.", request), err);
            throw new Exception(String.format("Failed to get the node with name '%s' in application '%s'.", application, name));
        }
    }

    /**
     * Look for a save node in the application
     *
     * @param neo4jAL Neo4j Access Layer
     * @param id      Id of the backup
     * @return Optional of the node found
     */
    public static Optional<Node> findMasterSaveNodeById(Neo4jAL neo4jAL, Long id) throws Exception {
        String request = String.format("MATCH (o:`%s`) WHERE ID(o)=$id RETURN o as node;", getLabelAsString());

        try {
            // Look for an existing object in the database
            Node n = null;
            Result res = neo4jAL.executeQuery(request, Map.of("id", id));
            if (res.hasNext()) return Optional.of((Node) res.next().get("node"));
            else return Optional.empty();

        } catch (Neo4jQueryException err) {
            neo4jAL.logError(String.format("Failed to delete the save node. Request : '%s'.", request), err);
            throw new Exception(String.format("Failed to get the node with id '%d' .", id));
        }
    }


    /**
     * Look for a save node in the application
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @param name        Name of the save
     * @return Optional of the node found
     */
    public static Node createMasterSaveNode(Neo4jAL neo4jAL, String application, String name) throws Exception {
        MasterSaveNode ms = new MasterSaveNode(name);
        return ms.createNode(neo4jAL, application);
    }

    /**
     * Merge a Save node
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @param name        Name of the node
     * @return
     * @throws Exception
     */
    public static Node findOrCreateMasterSaveNode(Neo4jAL neo4jAL, String application, String name) throws Exception {
        // Look for an existing node
        Optional<Node> node = findMasterSaveNodeByName(neo4jAL, application, name);
        if (node.isPresent()) return node.get();
        else return createMasterSaveNode(neo4jAL, application, name); // Create the node is nod results
    }

    /**
     * Get the list of saves nodes in the application
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @return The list of saves for this application
     * @throws Exception
     */
    public static List<MasterSaveNode> getListMasterSave(Neo4jAL neo4jAL, String application) throws Exception {
        String req = String.format("MATCH (s:`%s`:`%s`) " +
                "RETURN DISTINCT s as node", application, getLabelAsString());

        try {
            List<MasterSaveNode> returnList = new ArrayList<>();
            Result res = neo4jAL.executeQuery(req);

            while (res.hasNext()) {
                Node n = (Node) res.next().get("node");
                try {
                    returnList.add(new MasterSaveNode(n));
                } catch (Neo4jBadNodeFormatException e) {
                    neo4jAL.logError("Failed to convert node to MasterNode", e);
                }
            }

            return returnList;
        } catch (Neo4jQueryException e) {
            neo4jAL.logError(String.format("Failed to get the list of saves in the application '%s'", application), e);
            throw new Exception("Failed to get the list of saves");
        }
    }

    /**
     * Save a list of nodes
     *
     * @param neo4jAL     Neo4j Access layer
     * @param application Name of the application
     * @param name        Name of the save
     * @param taxonomy    Taxonomy to be save
     * @param idList      List of node to attach
     */
    public static MasterSaveNode saveObjects(Neo4jAL neo4jAL, String application, String name, String taxonomy, List<Long> idList) throws Exception, Neo4jBadNodeFormatException {
        // Find or create the master node
        Node master = findOrCreateMasterSaveNode(neo4jAL, application, name);

        // Find or create a save node attached to this master nodes
        Node saveNode = SaveNodeService.findOrCreate(neo4jAL, master.getId(), taxonomy);

        // Attach the nodes
        SaveNodeService.attachNodes(neo4jAL, saveNode.getId(), idList);
        return new MasterSaveNode(master);
    }

    /**
     * Delete a master node and all its attached save nodes
     *
     * @param neo4jAL Neo4j Access Layer
     * @param id      Id of the backup to delete
     */
    public static void deleteMasterSave(Neo4jAL neo4jAL, Long id) throws Exception {
        try {
            // Find node
            Optional<Node> masterSave = findMasterSaveNodeById(neo4jAL, id);
            if (masterSave.isEmpty()) return; // No save to delete
            Node n = masterSave.get();

            // Remove the save nodes attached
            SaveNodeService.deleteAttached(neo4jAL, n.getId());

            try {
                // Delete the node
                n.delete();
            } catch (Exception e) {
                neo4jAL.logError("Failed to delete the master save node", e);
                throw new Exception("Failed to delete the master save node itself.");
            }
        } catch (Exception e) {
            neo4jAL.logError("Failed to remove a save from the application.", e);
            throw new Exception(String.format("Failed to remove backup with id '%d'.", id));
        }
    }

    /**
     * Get the list of nodes in the application
     *
     * @param neo4jAL     Neo4j Access Layer
     * @param application Name of the application
     * @param idBackup    Id of the backup
     * @return A mapping between the taxonomy and the list of nodes to move
     * @throws Exception
     */
    public static Map<String, List<Long>> getDifferences(Neo4jAL neo4jAL, String application, Long idBackup) throws Exception {
        try {
            // Find node
            Optional<Node> masterSave = findMasterSaveNodeById(neo4jAL, idBackup);
            if (masterSave.isEmpty())
                throw new Exception(String.format("The backup with id [%d] doesn't exist", idBackup));

            // Get the node
            Node n = masterSave.get();

            // Get all save nodes
            Map<String, List<Long>> taxonomyMap = new HashMap<>();

            // Get the list of difference for each nodes
            String taxonomy;
            for (Node node : SaveNodeService.getAttached(neo4jAL, n.getId())) {
                taxonomy = SaveNodeService.getNodeTaxonomyById(neo4jAL, node.getId());
                taxonomyMap.put(taxonomy, SaveNodeService.getDifferences(neo4jAL, application, node.getId()));
            }

            // Stats and filter
            int count = taxonomyMap.size();
            neo4jAL.logInfo(String.format("%d Groups have been identified.", count));
            taxonomyMap = taxonomyMap.entrySet().stream().filter(x -> x.getValue().size() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            neo4jAL.logInfo(String.format("%d Groups contains nodes to reassign ( Difference %d ).",
                    taxonomyMap.size(), count - taxonomyMap.size()));

            return taxonomyMap;
        } catch (Exception e) {
            neo4jAL.logError("Failed to rollback the application.", e);
            throw new Exception(String.format("Failed to rollback application '%s'.", application));
        }
    }

}
