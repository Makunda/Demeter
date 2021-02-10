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
	 * @param neo4jAL Neo
	 * @return
	 */
	private static  List<Node> groupInAllApplications(Neo4jAL neo4jAL,  Class<? extends AGrouping> controller, String prefix) throws Neo4jQueryException {

		String applicationReq =
				"MATCH (o:Object) WHERE EXISTS (o.Tags) AND any(x in o.Tags WHERE x CONTAINS $tagPrefix) "
						+ "RETURN DISTINCT [ x in LABELS(o) WHERE NOT x='Object'][0] as application;";
		Map<String, Object> params = Map.of("tagPrefix", prefix);

		Result res = neo4jAL.executeQuery(applicationReq, params);
		List<Node> fullResults = new ArrayList<>();

		// Parse all the applications
		Class[] cArg = new Class[2];
		cArg[0] = Neo4jAL.class;
		cArg[1] = String.class;

		String application;
		while (res.hasNext()) {
			application = (String) res.next().get("application");

			try {
				AGrouping instance = controller.getDeclaredConstructor(cArg).newInstance(neo4jAL, application);
				fullResults.addAll(instance.launch());
			} catch (Exception | Neo4jBadRequestException e) {
				neo4jAL.logError(String.format("Failed to group application %s", application), e);
			}
		}

		return fullResults;
	}

	// Modules
	public static List<Node> groupAllModules(Neo4jAL neo4jAL) throws Neo4jQueryException {
		String prefix = Configuration.getBestOfALl("demeter.prefix.module_group");
		return groupInAllApplications(neo4jAL, ModuleGroupController.class, prefix);
	}

	// Architectures
	public static  List<Node> groupAllArchitecture(Neo4jAL neo4jAL) throws  Neo4jQueryException{
		String prefix = Configuration.getBestOfALl("demeter.prefix.architecture_group");
		return groupInAllApplications(neo4jAL, ArchitectureGroupController.class, prefix);
	}
}
