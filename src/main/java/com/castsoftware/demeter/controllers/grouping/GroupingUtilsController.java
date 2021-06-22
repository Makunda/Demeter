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
import com.castsoftware.demeter.controllers.grouping.architectures.ArchitectureGroupController;
import com.castsoftware.demeter.controllers.grouping.modules.ModuleGroupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.demeter.CandidateFindingResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupingUtilsController {


	/**
	 * Retrieve the list of applications containing Modules tags
	 * @param neo4jAL Neo4j Access Layer
	 * @param prefix Prefix of the controleler
	 * @return The list of application
	 */
	private static List<String> getCandidatesApplications(Neo4jAL neo4jAL, String prefix) throws Neo4jQueryException {

		String applicationReq =
				"MATCH (o:Object) WHERE EXISTS (o.Tags) AND any(x in o.Tags WHERE x STARTS WITH $tagPrefix) "
						+ "RETURN DISTINCT [ x in LABELS(o) WHERE NOT x='Object'][0] as application;";
		Map<String, Object> params = Map.of("tagPrefix", prefix);

		Result res = neo4jAL.executeQuery(applicationReq, params);
		List<String> fullResults = new ArrayList<>();

		while (res.hasNext()) {
			fullResults.add((String) res.next().get("application"));
		}

		return fullResults;
	}

	/**
	 * Group all the modules found ( nodes with modules prefix ) across all the applications
	 * @param neo4jAL Neo4j Access Layer
	 * @return List of the module created
	 * @throws Neo4jQueryException If Neo4j Cypher requests are not valid
	 */
	public static List<Node> groupAllModules(Neo4jAL neo4jAL) throws Neo4jQueryException {
		String prefix = Configuration.getBestOfALl("demeter.prefix.module_group");
		List<Node> res = new ArrayList<>();

		for(String application : getCandidatesApplications(neo4jAL, prefix)) {
			try {
				res.addAll((new ModuleGroupController(neo4jAL, application)).launch());
			} catch (Neo4jBadRequestException e) {
				neo4jAL.logError(String.format("Failed to execute module grouping on application %s", application), e);
			}
		}
		return res;
	}

	/**
	 * Group all the Architecture level across all the applications
	 * @param neo4jAL Neo4j Access Layer
	 * @return The list of Architecture nodes created
	 * @throws Neo4jQueryException If Neo4j Cypher requests are not valid
	 */
	public static  List<Node> groupAllArchitecture(Neo4jAL neo4jAL) throws  Neo4jQueryException{
		String prefix = Configuration.getBestOfALl("demeter.prefix.architecture_group");
		List<Node> res = new ArrayList<>();

		for(String application : getCandidatesApplications(neo4jAL, prefix)) {
			try {
				res.addAll((new ArchitectureGroupController(neo4jAL, application)).launch());
			} catch (Neo4jBadRequestException e) {
				neo4jAL.logError(String.format("Failed to execute module grouping on application %s", application), e);
			}
		}

		return res;
	}

	/**
	 * Refresh a specific architecture in the application
	 * @param neo4jAL Neo4j access layer
	 * @param application Name of the application
	 * @param archiName Architecture name
	 * @throws Neo4jQueryException If Neo4j Cypher requests are not valid
	 */
	public static void refreshArchitecture(Neo4jAL neo4jAL, String application, String archiName) throws  Neo4jQueryException{
		ArchitectureGroupController ag = new ArchitectureGroupController(neo4jAL, application);

		ag.refreshSubset(archiName);
		ag.refreshArchiModel(archiName);
	}
}
