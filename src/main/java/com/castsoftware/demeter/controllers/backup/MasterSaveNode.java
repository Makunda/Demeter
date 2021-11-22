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

package com.castsoftware.demeter.controllers.backup;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.sql.Timestamp;
import java.util.*;

public class MasterSaveNode {

	private static final String MASTERSAVE_NODE_LABEL = "DemeterMasterSave";
	private static final String TO_SAVE_NODE = "DECLARES";

	/**
	 * Get the label of the node to save
	 * @return The label as string
	 */
	public static String getLabelAsString() {
		return MASTERSAVE_NODE_LABEL;
	}

	/**
	 * Get the label of the node to save
	 * @return The label as label
	 */
	public static Label getLabel() {
		return Label.label(MASTERSAVE_NODE_LABEL);
	}

	/**
	 * Get the relationship between master and save node
	 * @return The name of the relationship
	 */
	public static String getRelationshipToSaveNode() {
		return TO_SAVE_NODE;
	}

	/**
	 * Look for a save node in the application
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param name Name of the save
	 * @return Optional of the node found
	 */
	public static Optional<Node> findMasterSaveNodeByName(Neo4jAL neo4jAL, String application, String name) throws Exception {
		String request  = String.format("MATCH (o:`%s`:`%s`) WHERE o.Name=$name RETURN o as node;", application, getLabelAsString());

		try {
			// Look for an existing object in the database
			Node n = null;
			Result res = neo4jAL.executeQuery(request, Map.of("name", name));
			if(res.hasNext()) return Optional.of((Node) res.next().get("node"));
			else return Optional.empty();

		} catch (Neo4jQueryException err) {
			neo4jAL.logError(String.format("Failed to get or create the save node. Request : '%s'.", request), err);
			throw  new Exception(String.format("Failed to get the node with name '%s' in application '%s'.", application, name));
		}
	}


	/**
	 * Look for a save node in the application
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param name Name of the save
	 * @return Optional of the node found
	 */
	public static Node createMasterSaveNode(Neo4jAL neo4jAL, String application, String name, String description) throws Exception {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String stringTimestamp = timestamp.toString();

		// Create request
		String request  = String.format("CREATE (o:`%s`:`%s`) " +
				"SET o.Name=$name " +
				"SET o.Description=$description " +
				"SET o.Timestamp=$timestamp " +
				"RETURN o as node;", application, getLabelAsString());
		Map<String, Object> params = Map.of("name", name, "description", description, "timestamp", stringTimestamp);

		try {
			// Create a new node in the database and throw an error if it doesn't exist
			Node n;
			Result res = neo4jAL.executeQuery(request,params);
			if(res.hasNext()) n = (Node) res.next().get("node");
			else throw  new Exception("Failed to create the save node. No results.");

			return n;
		} catch (Neo4jQueryException | Exception err) {
			neo4jAL.logError(String.format("Failed to create the save node. Request : '%s'.", request), err);
			throw  new Exception(String.format("Failed to create the node with name '%s' in application '%s'.", application, name));
		}
	}

	/**
	 * Merge a Save node
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param name Name of the node
	 * @param description Description of the node ( Optional )
	 * @return
	 * @throws Exception
	 */
	public static Node findOrCreateMasterSaveNode(Neo4jAL neo4jAL, String application, String name, String description) throws Exception {
		// Look for an existing node
		Optional<Node> node = findMasterSaveNodeByName(neo4jAL, application, name);
		if(node.isPresent()) return node.get();
		else return createMasterSaveNode(neo4jAL, application, name, description); // Create the node is nod results
	}

	/**
	 * Get the list of saves nodes in the application
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @return The list of saves for this application
	 * @throws Exception
	 */
	public static List<String> getListMasterSave(Neo4jAL neo4jAL, String application) throws Exception {
		String req = String.format("MATCH (s:`%s`:`%s`) " +
						"RETURN s.Name as name", application, getLabelAsString());

		try {
			List<String> returnList = new ArrayList<>();
			Result res = neo4jAL.executeQuery(req);

			while (res.hasNext()) {
				returnList.add((String) res.next().get("name"));
			}

			return returnList;
		} catch (Neo4jQueryException e) {
			neo4jAL.logError(String.format("Failed to get the list of saves in the application '%s'", application), e);
			throw new Exception("Failed to get the list of saves");
		}
	}

	/**
	 * Save a list of nodes
	 * @param neo4jAL Neo4j Access layer
	 * @param application Name of the application
	 * @param name Name of the save
	 * @param taxonomy Taxonomy to be save
	 * @param idList List of node to attach
	 */
	public static void saveObjects(Neo4jAL neo4jAL, String application, String name, String taxonomy, List<Long> idList) throws Exception {
		// Find or create the master node
		Node master = findOrCreateMasterSaveNode(neo4jAL, application, name, "");

		// Find or create a save node attached to this master nodes
		Node saveNode  = SaveNode.findOrCreate(neo4jAL, master.getId(), taxonomy);

		// Attach the nodes
		SaveNode.attachNodes(neo4jAL, saveNode.getId(), idList);
	}

	/**
	 * Delete a master node and all its attached save nodes
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param save Name of the save
	 */
	public static void deleteMasterSave(Neo4jAL neo4jAL, String application, String save) throws Exception {
		try {
			// Find node
			Optional<Node> masterSave = findMasterSaveNodeByName(neo4jAL, application, save);
			if(masterSave.isEmpty()) return; // No save to delete
			Node n = masterSave.get();

			// Remove the save nodes attached
			SaveNode.deleteAttached(neo4jAL, n.getId());

			try {
				// Delete the node
				n.delete();
			} catch (Exception e) {
				neo4jAL.logError("Failed to delete the master save node", e);
				throw new Exception("Failed to delete the master save node itself.");
				}
		} catch (Exception e) {
				neo4jAL.logError("Failed to remove a save from the application.", e);
				throw new Exception(String.format("Failed to remove a save from '%s'.", application));
		}
	}

	/**
	 * Get the list of nodes in the application
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param save Name of the save
	 * @return A mapping between the taxonomy and the list of nodes to move
	 * @throws Exception
	 */
	public static Map<String, List<Long>> getDifferences(Neo4jAL neo4jAL, String application, String save) throws Exception {
		try {
			// Find node
			Optional<Node> masterSave = findMasterSaveNodeByName(neo4jAL, application, save);
			if(masterSave.isEmpty()) throw new Exception("The save specified doesn't exist");

			// Get the node
			Node n = masterSave.get();

			// Get all save nodes
			List<Node> saveNodes = SaveNode.getAttached(neo4jAL, n.getId());
			Map<String, List<Long>> taxonomyMap = new HashMap<>();

			// Get the list of difference for each nodes
			String taxonomy;
			for (Node node : SaveNode.getAttached(neo4jAL, n.getId())) {
				taxonomy = SaveNode.getNodeTaxonomyById(neo4jAL, node.getId());
				taxonomyMap.put(taxonomy, SaveNode.getDifferences(neo4jAL, application, node.getId()));
			}

			return taxonomyMap;
		} catch (Exception e) {
			neo4jAL.logError("Failed to rollback the application.", e);
			throw new Exception(String.format("Failed to rollback application '%s'.", application));
		}
	}

}