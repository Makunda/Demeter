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

package com.castsoftware.demeter.controllers.grouping.aggregations;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import picocli.CommandLine;

import java.util.*;

public class AggregationController {

	private Neo4jAL neo4jAL;
	private String application;

	private static final String AGGREGATION_LABEL = "CustomView";
	private static final String CUSTOM_LABEL = "Custom";

	/**
	 * Verify if the nodes provided are CUSTOM Node
	 * @param idNodes List of nodes to validate
	 * @return The list of verify nodes
	 */
	private List<Long> verifyNodes(List<Long> idNodes) throws Neo4jQueryException {
		String req = String.format("MATCH (l:%s:`%s`) WHERE ID(l)=$id RETURN l as node", CUSTOM_LABEL, application);
		List<Long> returnList = new ArrayList<>();

		Result res;
		Map<String, Object> params;
		for(Long id : idNodes) { // Iterate over the id, and test them
			try {
				params = Map.of("id", id);
				res = this.neo4jAL.executeQuery(req, params);
				if(res.hasNext()) returnList.add(id);
			} catch (Neo4jQueryException error) {
				this.neo4jAL.logError("Node verification produced an error.", error);
				throw error;
			}
		}

		// Return the list of valid elements
		return returnList;
	}

	/**
	 * Find a custom aggregation node by its name in an application
	 * @param aggregationName Name of the aggregation
	 * @return Node found, or null
	 */
	public Optional<Node> findAggregationNodeByName(String aggregationName) throws Neo4jQueryException {
		String req = String.format("MATCH (a:%s:`%s`) WHERE a.Name=$name RETURN a as node", AGGREGATION_LABEL, application);
		Map<String, Object> params = Map.of("name", aggregationName);

		try {
			Result res = this.neo4jAL.executeQuery(req, params);
			if(res.hasNext()) {
				Node node = (Node) res.next().get("node");
				return Optional.of(node);
			} else {
				return Optional.empty();
			}
		} catch (Neo4jQueryException error) {
			this.neo4jAL.logError("Failed to find aggregation node by name.", error);
			throw error;
		}
	}

	/**
	 * Find a custom aggregation node by its id in an application
	 * @param aggregationId ID of the aggregation
	 * @return Node found, or null
	 */
	public Optional<Node> findAggregationNodeById(Long aggregationId) throws Neo4jQueryException {
		String req = String.format("MATCH (a:%s:`%s`) WHERE ID(a)=$id RETURN a as node", AGGREGATION_LABEL, application);
		Map<String, Object> params = Map.of("id", aggregationId);

		try {
			Result res = this.neo4jAL.executeQuery(req, params);
			if(res.hasNext()) {
				Node node = (Node) res.next().get("node");
				return Optional.of(node);
			} else {
				return Optional.empty();
			}
		} catch (Neo4jQueryException error) {
			this.neo4jAL.logError("Failed to find aggregation node by id.", error);
			throw error;
		}
	}

	/**
	 * Merge an existing aggregation to the rest of the listed nodes
	 * @param idAggregationNode id of the node to be merged
	 * @param idNodes List of nodes to merge
	 */
	private void mergeAggregationNode(Long idAggregationNode, List<Long> idNodes) throws Neo4jQueryException {
		String req = String.format("MATCH (a:%1$s:`%2$s`) WHERE ID(a)=$id " +
						"WITH a LIMIT 1 " +
						"MATCH (l:%3$s:`%2$s`) WHERE ID(l) IN $idList " +
						"MERGE (a)-[:HAS]->(l)", AGGREGATION_LABEL, application, CUSTOM_LABEL);
		Map<String, Object> params = Map.of("id", idAggregationNode, "idList", idNodes);

		try {
			this.neo4jAL.executeQuery(req, params);
		} catch (Neo4jQueryException error) {
			this.neo4jAL.logError("Failed to find aggregation node by name.", error);
			throw error;
		}
	}

	/**
	 * Create a new custom aggregation node
	 * @param aggregationName
	 * @return The node created
	 */
	private Node createAggregationNode(String aggregationName) throws Neo4jQueryException, Neo4jNoResult {
		String req = String.format("CREATE  (a:%1$s:`%2$s`) " +
						"SET a.AggregationDepth=1 " +
						"SET a.Published=true " +
						"SET a.CreatedBy=\"Atlas\" " +
						"SET a.Collaborators=[] " +
						"SET a.Name=$name " +
						"RETURN a as node", AGGREGATION_LABEL, application);
		Map<String, Object> params = Map.of("name", aggregationName);
		Result res = this.neo4jAL.executeQuery(req, params);

		// If the creation failed throw an exception
		if(!res.hasNext()) {
			this.neo4jAL.logError("Failed to create a custom aggregation. The request returned no results");
			throw new Neo4jNoResult("Failed to create a new aggregation node", req, "AGGCxCREAA01");
		}

		return (Node) res.next().get("node");
	}

	/**
	 * Create a new custom aggregation
	 * @param aggregationName Name of the aggregation
	 * @return The node created, or null if the operation failed
	 */
	public Node createAggregation(String aggregationName) throws Neo4jQueryException, Neo4jNoResult {
		try {
			// Find or create the aggregation node
			Optional<Node> aggNode = this.findAggregationNodeByName(aggregationName);
			Node aggregationNode;

			// Test the result
			if(aggNode.isEmpty()) {
				// Create a new node if the find failed
				aggregationNode = this.createAggregationNode(aggregationName);
			} else {
				aggregationNode = aggNode.get();
			}

			return aggregationNode;
		} catch (Exception | Neo4jQueryException | Neo4jNoResult err) {
			this.neo4jAL.logError("Failed to create the aggregation node", err);
			throw err;
		}
	}

	/**
	 * Create custom nodes
	 * @param name Name of the custom node to create
	 * @param idNodes Id of the objects to attach
	 * @return The created custom node
	 */
	private Node createCustomNode(String name, List<Long> idNodes) throws Neo4jNoResult, Neo4jQueryException {

		String reqCustom = String.format("CREATE (c:%s:`%s`) " +
				"SET c.Name=$name " +
				"SET c.Count=$count " +
				"SET c.Level=$level " +
				"SET c.FullName=$name " +
				"RETURN c as node", CUSTOM_LABEL, application);
		Map<String, Object> customParams = Map.of(
				"name", name,
				"level", 1,
				"count", idNodes.size(),
				"fullName", name);

		Result resCustom = this.neo4jAL.executeQuery(reqCustom, customParams);
		if(!resCustom.hasNext()) {
			this.neo4jAL.logError("Failed to create a custom node. The request return no results.");
			throw new Neo4jNoResult("Failed to create a custom node. The request return no results.", reqCustom, "AGGCxCREAC01");
		}

		return (Node) resCustom.next().get("node");
	}

	/**
	 * Link the objects to the custom node
	 * @param customNode Name of the custom node
	 * @param idNodes Id of the nodes
	 */
	private void mergeCustomNodeRelationships(Node customNode,  List<Long> idNodes) throws Neo4jQueryException {
		// Link the objects
		String reqLinks = String.format("MATCH (a:%s:`%2$s`) WHERE ID(a)=$id " +
				"WITH a " +
				"MATCH (o:Object:`%2$s`) WHERE ID(o)=$idObj " +
				"MERGE (a)-[:Aggregates]->(o)", CUSTOM_LABEL, application);

		Map<String, Object> paramsLinks;
		int count = 0;
		for(Long id : idNodes) {
			paramsLinks = Map.of("id", customNode.getId(), "idObj", id);
			this.neo4jAL.executeQuery(reqLinks, paramsLinks);
			count ++;
		}

		neo4jAL.logInfo(String.format("Executed %d queries to link the " +
				"objects for custom node with id '%d' ", count, customNode.getId()));

	}

	/**
	 * Find a custom node using it's name and the name of the parent aggregation
	 * @param aggregationId Id of the aggregation
	 * @param customName Name of the custom node
	 * @return The node, or null if not found
	 */
	private  Optional<Node> findCustomNode(Long aggregationId, String customName) throws Neo4jQueryException {
		String reqLinks = String.format("MATCH (a:%s:`%2$s`) WHERE ID(a)=$id " +
				"WITH a " +
				"MATCH (a)-[]->(c:%3$s:`%2$s`) WHERE c.Name=$customName " +
				"RETURN c as node", AGGREGATION_LABEL, application, CUSTOM_LABEL);
		Map<String, Object> paramsLinks = Map.of("id", aggregationId, "customName", customName);
		Result res = this.neo4jAL.executeQuery(reqLinks, paramsLinks);

		if(!res.hasNext()) return Optional.empty();

		Node node = (Node) res.next().get("node");
		return Optional.of(node);
	}

	/**
	 * Link a custom node to the aggregation node
	 * @param aggregationId Id of the aggregation node
	 * @param customId If of the custom node
	 */
	private void linkCustomToAggregate(Long aggregationId, Long customId) throws Neo4jQueryException {
		try {
			String req = String.format("MATCH (a:%1$s:`%2$s`) WHERE ID(a)=$aggregationId " +
					"WITH a " +
					"MATCH (c:%3$s:`%2$s`) WHERE ID(c)=$customId " +
					"MERGE (a)-[:HAS]->(c)", AGGREGATION_LABEL, application, CUSTOM_LABEL);
			Map<String, Object> params = Map.of("aggregationId", aggregationId,
					"customId", customId);
			neo4jAL.executeQuery(req, params);
		} catch (Neo4jQueryException error) {
			neo4jAL.logError("Failed to link the custom node to the aggregation node", error);
			throw error;
		}
	}

	/**
	 * Create a custom node from a list of object
	 * @param aggregationId Id of the aggregation node
	 * @param customName Name of the custom node
	 * @param idNodes Id of nodes to merge
	 * @return The node created
	 * @throws Neo4jQueryException
	 * @throws Neo4jNoResult
	 */
	public Node createCustom(Long aggregationId, String customName, List<Long> idNodes) throws Neo4jQueryException, Neo4jNoResult {
		// Find the aggregation node
		Optional<Node> aggregationNode = this.findAggregationNodeById(aggregationId);
		if(aggregationNode.isEmpty()) { // If the aggregation is not found throw an error
			this.neo4jAL.logError(String.format("The aggregation node with id '%d' " +
					"doesn't exist in the application '%s'", aggregationId, application));
			throw new Neo4jNoResult("Failed to find Aggregation node.", "Check the logs", "AGGCxCREAC01");
		}

		// Get the list of objects to link to the custom view
		String req = String.format("MATCH (o:`%s`:Object) WHERE ID(o) IN $idNode RETURN ID(o) as idNode", application);
		Map<String, Object> params = Map.of("idNode", idNodes);

		// Validate the ID and get the list of compliant node IDs
		Result res = this.neo4jAL.executeQuery(req, params);
		List<Long> nodeIdList = new ArrayList<>();
		while(res.hasNext()) {
			nodeIdList.add((Long) res.next().get("idNode"));
		}
		nodeIdList.removeAll(Collections.singleton(null));

		this.neo4jAL.logInfo(String.format("Creating a custom nodes with %d objects attached.", nodeIdList.size()));

		// create or merge
		Node node;
		Optional<Node> optNode = this.findCustomNode(aggregationId, customName);
		if(optNode.isPresent()) {
			node = optNode.get();
		} else {
			node = this.createCustomNode(customName, idNodes);
		}

		// Merge the relationships
		this.mergeCustomNodeRelationships(node, nodeIdList);

		// Attach it to the aggregation node
		this.linkCustomToAggregate(aggregationId, node.getId());

		return node;
	}

	/**
	 * Delete a custom node by its id
	 * @param customId Id of the custom node to delete
	 * @throws Neo4jQueryException
	 */
	public void deleteCustomById(Long customId) throws Neo4jQueryException {
		this.neo4jAL.deleteByIdAndLabel(customId, CUSTOM_LABEL);
	}

	/**
	 * Get the number of links between custom nodes
	 * @param custom1 Id of the first node
	 * @param custom2 Id of the second node
	 * @return The number of links between the two nodes
	 */
	private void getLinksNumber(Long custom1, Long custom2) throws Neo4jQueryException {
		String refreshLinks = String.format("MATCH (l:%2$s:`%1$s`)-[]->(o)-->(o2)<-[]-(l2:%2$s:`%1$s`) " +
				"WHERE ID(l)=$id1 and ID(l2)=$id2 " +
				"AND ID(l)<>ID(l2) " +
				"WITH l, l2, o LIMIT 1 " +
				"MERGE (l)-[:References]->(l2); ", application, CUSTOM_LABEL);
		Map<String, Object> params = Map.of("id1", custom1, "id2", custom2);
		neo4jAL.executeQuery(refreshLinks, params);
	}

	/**
	 * Refresh the aggregation, and link the custom views
	 * @param idAggregation Id of the aggregation
	 */
	public void refreshAggregation(Long idAggregation) throws Neo4jQueryException {
		List<Node> nodes = this.getCustomNodesAttached(idAggregation);

		for(Node n1 : nodes) { // first nodes loop
			for(Node n2 : nodes) {
				if(n1.getId() == n2.getId()) continue;  // Skip if same node

				// Check if nodes are linked
				this.getLinksNumber(n1.getId(), n2.getId());
			}
		}

		this.neo4jAL.logInfo(String.format("The aggregation with id '%d' has been refreshed.", idAggregation));
	}

	/**
	 * Get the list of Custom nodes attached to the aggregation
	 * @param idAggregation Id of the aggregation to explore
	 * @return The list of Custom nodes linked
	 */
	public List<Node> getCustomNodesAttached(Long idAggregation) throws Neo4jQueryException {
		String req = String.format("MATCH (a:%1$s:`%2$s`)-[]->(c:%3$s) " +
						"WHERE ID(a)=$id " +
						"RETURN  c as node", AGGREGATION_LABEL, application, CUSTOM_LABEL);
		Map<String, Object> params = Map.of("id", idAggregation);
		Result res = this.neo4jAL.executeQuery(req, params);

		List<Node> nodeList = new ArrayList<>();
		while(res.hasNext()) { // Iterate and get the nodes
			nodeList.add((Node) res.next().get(("node")));
		}

		return nodeList;
	}

	/**
	 * Delete a specific aggregation node by its
	 * @param aggregationName Name of the aggregation
	 * @throws Neo4jQueryException
	 */
	public void deleteAggregationByName(String aggregationName) throws Neo4jQueryException {
		// Find or create the aggregation node
		Optional<Node> aggNode = this.findAggregationNodeByName(aggregationName);

		// Delete customs nodes
		if(aggNode.isPresent()) {
			this.neo4jAL.logInfo(String.format("An aggregation node with name '%s' in application '%s' " +
					"has been found and will now be deleted.", aggregationName, application));
			List<Node> nodeList = this.getCustomNodesAttached(aggNode.get().getId());
			for(Node node : nodeList) {
				this.neo4jAL.deleteNodeById(node.getId());
			}

			this.neo4jAL.deleteNodeById(aggNode.get().getId());
		} else {
			this.neo4jAL.logError(String.format("No aggregation node with name '%s' was found in application '%s'.",
					aggregationName, application));
		}
	}

	public AggregationController(Neo4jAL neo4jAL, String application) {
		this.neo4jAL = neo4jAL;
		this.application = application;
	}

}
