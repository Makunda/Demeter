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
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.services.backup.MasterSaveNodeService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaveNodeService {

	private static final String LABEL = "DemeterSave";
	private static final String BACKUP_RELATIONSHIP = "BACKED_BY";


	/**
	 * Get the label of the node as a String
	 * @return The node label
	 */
	public static String getLabelAsString() {
		return LABEL;
	}

	/**
	 * Find or create a node in the database
	 * @param masterId Id of the master node
	 * @param taxonomy Taxonomy to save
	 * @return
	 */
	public static Node findOrCreate(Neo4jAL neo4jAL, Long masterId, String taxonomy) throws Exception {
		// Get external properties
		String masterLabel = MasterSaveNodeService.getLabelAsString();
		String relationship = MasterSaveNodeService.getRelationshipToSaveNode();

		// Declare requests
		String req = String.format("MATCH (mast:`%s`)-[:%s]->(o:`%s`) WHERE ID(mast)=$idMast AND o.Taxonomy=$taxonomy RETURN o as node",
				masterLabel, relationship, getLabelAsString());

		String createReq = String.format("MATCH (mast:`%s`) WHERE ID(mast)=$idMast " +
				"WITH mast " +
				"CREATE (o:`%s`) " +
				"SET o.Taxonomy=$taxonomy " +
				"MERGE (mast)-[:%s]->(o) " +
				"RETURN DISTINCT o as node", masterLabel, getLabelAsString(), relationship);

		// Declare parameters
		Map<String, Object> params = Map.of("idMast", masterId, "taxonomy", taxonomy);

		try {
			// Try to find the node
			Result res = neo4jAL.executeQuery(req, params);
			if(res.hasNext()) {
				return (Node) res.next().get("node");
			}
		} catch (Neo4jQueryException e) {
			neo4jAL.logError(String.format("Request to find a node attached to master save with id [%d] failed.", masterId), e);
			throw new Exception("Request to find a node attached to master produced an error.");
		}

		// If nothing has been found, create the node
		try {
			Result res = neo4jAL.executeQuery(createReq, params);
			if(res.hasNext()) return (Node) res.next().get("node");
			else throw new Exception("The creation request returned no results.");
		} catch (Neo4jQueryException | Exception e) {
			neo4jAL.logError(String.format("Request to create a node attached to master save with id [%d] failed.", masterId), e);
			throw new Exception("Request to create a node attached to master produced an error.");
		}
	}

	/**
	 * Attach nodes to the save node
	 * @param neo4jAL Neo4j Access Layer
	 * @param idSave Id of the save node
	 * @param idList List of node id to attach
	 */
	public static void attachNodes(Neo4jAL neo4jAL, Long idSave, List<Long> idList) throws Exception {
		// Declare attach request
		String request  = String.format("MATCH (o:`%s`) WHERE ID(o)=$idSave " +
						"WITH o as saveNode " +
						"UNWIND $idObjects as idObj " +
						"MATCH (o:Object) WHERE id(o)=idObj " +
						"MERGE (saveNode)-[:%s]->(o) " +
						"RETURN DISTINCT o as node", getLabelAsString(), BACKUP_RELATIONSHIP);
		// List of parameters
		Map<String, Object> params = Map.of("idSave", idSave, "idObjects", idList);

		try {
			neo4jAL.executeQuery(request, params);
		} catch (Neo4jQueryException e) {
			neo4jAL.logError("Failed to attach nodes to the save.", e);
			throw new Exception("Failed to attach the list of nodes to the save node");
		}
	}

	/**
	 * Delete all the saves nodes attached to a master node
	 * @param neo4jAL Neo4j Access layer
	 * @param masterId Id of the master layer
	 */
	public static void deleteAttached(Neo4jAL neo4jAL, Long masterId) {
		// Get external properties
		String masterLabel = MasterSaveNodeService.getLabelAsString();
		String relationship = MasterSaveNodeService.getRelationshipToSaveNode();

		String request = String.format("MATCH (o:`%s`)-[:%s]->(s:`%s`) " +
						"WHERE ID(o)=$idNode " +
						"DETACH DELETE s", masterLabel, relationship, getLabelAsString());

		Map<String, Object> params = Map.of("idNode", masterId);

		try {
			neo4jAL.executeQuery(request, params);
		} catch (Neo4jQueryException e) {
			neo4jAL.logError(String.format("Failed to delete the save nodes attached to master node with id [%d].", masterId));
		}
	}

	/**
	 * Get all the saves nodes attached to a master node
	 * @param neo4jAL Neo4j Access layer
	 * @param masterId Id of the master layer
	 * @return The list of save nodes attached to a master node
	 */
	public static List<Node> getAttached(Neo4jAL neo4jAL, Long masterId) throws Exception {
		// Get external properties
		String masterLabel = MasterSaveNodeService.getLabelAsString();
		String relationship = MasterSaveNodeService.getRelationshipToSaveNode();

		String request = String.format("MATCH (o:`%s`)-[:%s]->(s:`%s`) " +
				"WHERE ID(o)=$idNode " +
				"RETURN DISTINCT s as node", masterLabel, relationship, getLabelAsString());

		Map<String, Object> params = Map.of("idNode", masterId);

		try {
			List<Node> nodeList = new ArrayList<>();
			Result res = neo4jAL.executeQuery(request, params);

			while (res.hasNext()) {
				nodeList.add((Node) res.next().get("node"));
			}

			return nodeList;
		} catch (Neo4jQueryException e) {
			neo4jAL.logError(String.format("Failed to delete the save nodes attached to master node with id [%d].", masterId));
			throw new Exception("Failed to get attached nodes.");
		}
	}

	/**
	 * Get the node taxonomy using its ID
	 * @param neo4jAL Neo4j Acces Layer
	 * @param idNode Id of the node to search
	 * @return
	 */
	public static String getNodeTaxonomyById(Neo4jAL neo4jAL, Long idNode) throws Exception {
		try {
			String getName = String.format("MATCH (s:`%1$s`) WHERE ID(s)=$idNode RETURN s.Taxonomy as name", getLabelAsString());
			Map<String, Object> params = Map.of("idNode", idNode);
			Result res = neo4jAL.executeQuery(getName, params);

			if(!res.hasNext()) throw new Exception(String.format("Save node with id [%d] has no Taxonomy property.", idNode));
			return (String) res.next().get("name");
		} catch (Neo4jQueryException | Exception e) {
			neo4jAL.logError(String.format("Failed to retrieve the taxonomy property of a save node with id [%d].", idNode), e);
			throw new Exception(String.format("Failed to retrieve the taxonomy property of a save node with id [%d].", idNode));
		}
	}

	/**
	 * Get the list of node to reassign
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @param idNode Id of the save node
	 * @return A Mapping between the ToBe Taxonomy and the list of nodes
	 * @throws Exception
	 */
	public static List<Long> getDifferences(Neo4jAL neo4jAL, String application, Long idNode) throws Exception {
		// Declare the request
		String request = String.format("MATCH (s:`%1$s`)-[:%2$s]->(o:Object)<-[:Aggregates]-(l:Level5) WHERE ID(s)=$idNode " +
						"AND s.Taxonomy<>l.FullName " +
						"RETURN DISTINCT ID(o) as idObj", getLabelAsString(), BACKUP_RELATIONSHIP);
		Map<String, Object> params = Map.of("idNode", idNode);


		try {
			List<Long> nodeIdList = new ArrayList<>();
			Result res = neo4jAL.executeQuery(request, params);

			// Parse the results and store it
			while (res.hasNext()) {
				nodeIdList.add((Long) res.next().get("idObj"));
			}

			return nodeIdList;
		} catch (Neo4jQueryException e) {
			neo4jAL.logError("Failed to execute query returning the differences.", e);
			throw new Exception("Failed to get differences");
		}
	}
}
