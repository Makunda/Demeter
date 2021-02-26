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

package com.castsoftware.demeter.controllers.grouping;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AGrouping {

	protected Neo4jAL neo4jAL;
	protected String applicationContext;

	public abstract String getTagPrefix();
	public abstract void setTagPrefix(String value) throws FileNotFoundException, MissingFileException;

	public abstract void refresh() throws Neo4jQueryException;
	public abstract Node group(String groupName, List<Node> nodeList) throws Neo4jQueryException, Neo4jBadRequestException;

	public List<Node> launch() throws Neo4jQueryException, Neo4jBadRequestException {
		List<Node> nodes = new ArrayList<>();
		Map<String, List<Node>> mapNode = getGroupList();

		for (Map.Entry<String, List<Node>> entry : mapNode.entrySet()) {
			Node n = group(entry.getKey(), entry.getValue());
			nodes.add(n);
		}

		// Refresh
		refresh();

		// Clean tags
		cleanTags();


		return nodes;
	}

	/**
	 * Get the list of groups
	 * @return
	 * @throws Neo4jQueryException
	 */
	public Map<String, List<Node>> getGroupList() throws Neo4jQueryException {
		// Get the list of nodes prefixed by dm_tag
		String forgedTagRequest =
				String.format(
						"MATCH (o:`%1$s`) WHERE any( x in o.Tags WHERE x CONTAINS $tagPrefix)  "
								+ "WITH o, [x in o.Tags WHERE x CONTAINS $tagPrefix][0] as g "
								+ "RETURN o as node, g as group;",
						applicationContext);
		Map<String, Object> params = Map.of("tagPrefix", getTagPrefix());

		Map<String, List<Node>> groupMap = new HashMap<>();
		Result res = neo4jAL.executeQuery(forgedTagRequest, params);
		// Build the map for each group as <Tag, Node list>
		while (res.hasNext()) {
			Map<String, Object> resMap = res.next();
			String group = (String) resMap.get("group");
			Node node = (Node) resMap.get("node");

			// Add to  the specific group
			if (!groupMap.containsKey(group)) {
				groupMap.put(group, new ArrayList<>());
			}

			groupMap.get(group).add(node);
		}

		neo4jAL.logInfo(String.format("%d module groups (Prefix: %s) were identified.", groupMap.size(), getTagPrefix()));
		return groupMap;
	}

	/**
	 * Clean the residual tags in the database
	 */
	public void cleanTags() throws Neo4jQueryException {
		// Once the operation is done, remove Demeter tag prefix tags
		String removeTagsQuery =
				String.format(
						"MATCH (o:`%1$s`) WHERE EXISTS(o.Tags)  SET o.Tags = [ x IN o.Tags WHERE NOT x CONTAINS $tagPrefix ] RETURN COUNT(o) as removedTags;",
						applicationContext);
		Map<String, Object> params = Map.of("tagPrefix", getTagPrefix());
		Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery, params);
		neo4jAL.logInfo("Cleaning Done !");
	}

	public AGrouping(Neo4jAL neo4jAL, String applicationContext) {
		this.neo4jAL = neo4jAL;
		this.applicationContext = applicationContext;
	}


}
