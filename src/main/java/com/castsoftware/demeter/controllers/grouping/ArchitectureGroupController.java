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

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchitectureGroupController extends AGrouping{


	public ArchitectureGroupController(Neo4jAL neo4jAL, String applicationContext) {
		super(neo4jAL, applicationContext);
	}

	@Override
	public String getTagPrefix() {
		return Configuration.getBestOfALl("demeter.prefix.architecture_group");
	}

	@Override
	public Node group(String groupName, List<Node> nodeList) throws Neo4jQueryException {
		String[] cleanedGroupName = groupName.replace(getTagPrefix(), "").split("\\$");
		if(cleanedGroupName.length < 2) return null;

		String nameView = cleanedGroupName[0];
		String nameSubset = cleanedGroupName[1];


		// Create archi model
		String modelIdReq = String.format("MATCH (n:ArchiModel:`%s`) RETURN MAX(n.ModelId) as maxVal", applicationContext);
		Result res = this.neo4jAL.executeQuery(modelIdReq);
		Long maxId = 1L;
		if(res.hasNext()) {
			maxId = (Long) res.next().get("maxVal");
			maxId += 1;
		}

		// Merge & update
		String req = String.format("MERGE (n:ArchiModel:`%s` { Type:'archimodel', Color:'rgb(34,199,214)',Name:$groupName} ) " +
						"CASE WHEN EXISTS(n.Count) THEN SET n.Count=n.Count + $count ELSE SET n.Count=$count END SET n.ModelId=$maxModelID " +
						"RETURN n as node;", applicationContext);
		Map<String, Object> params = Map.of("groupName", nameView, "count", new Long(nodeList.size()), "maxModelID", maxId.toString() )
		Result result = neo4jAL.executeQuery(req);
		Node n = (Node) result.next().get("node");

		// Create the subset



		// Add the objects and the SubObjects

	}


}
