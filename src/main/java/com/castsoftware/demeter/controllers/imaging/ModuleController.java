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

package com.castsoftware.demeter.controllers.imaging;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;

import java.util.Map;

public class ModuleController {

	private final Neo4jAL neo4jAL;

	/**
	 * Get the hidden prefix of Module
	 * @return The prefix of the Hidden module
	 */
	public static String getHiddenPrefix() {
		return Configuration.get("demeter.module.hidden.label");
	}

	/**
	 * Hide a module in the application
	 * @param id Id of the module to hide
	 */
	public void hideModuleById(Long id) throws Neo4jQueryException {
		String req = String.format("MATCH (m:Module) WHERE ID(m)=$IdNode " +
				"REMOVE m:Module SET m:`%1$s` " +
				"RETURN m as node", getHiddenPrefix());
		Map<String, Object> params = Map.of("IdNode", id);
		this.neo4jAL.executeQuery(req, params);
	}

	/**
	 * Display a module in the application
	 * @param id Id of the module to hide
	 */
	public void displayModuleById(Long id) throws Neo4jQueryException {
		String req = String.format("MATCH (m:`%1$s`) WHERE ID(m)=$IdNode " +
				"REMOVE m:`%1$s` SET m:Module " +
				"RETURN m as node", getHiddenPrefix());
		Map<String, Object> params = Map.of("IdNode", id);
		this.neo4jAL.executeQuery(req, params);
	}

	/**
	 * Delete a module
	 * @param id Id of the module to hide
	 */
	public void deleteModule(Long id) throws Neo4jQueryException {
		Map<String, Object> params = Map.of("IdNode", id);

    	String req =
        	"MATCH (m:Module) WHERE ID(m)=$IdNode "
            + "OPTIONAL MATCH (m)-[r:Contains]->(o) WHERE o:Object OR o:SubObject "
            + "SET o.Module = CASE WHEN o.Module IS NULL THEN [] ELSE [ x in o.Module WHERE NOT x=m.Name ] END  ";
		this.neo4jAL.executeQuery(req, params);

		String reqDelete = "MATCH (m:Module) WHERE ID(m)=$IdNode DETACH DELETE m";
		this.neo4jAL.executeQuery(reqDelete, params);
	}


	/**
	 * Constructor
	 * @param neo4jAL Neo4j Access Layer
	 */
	public ModuleController(Neo4jAL neo4jAL) {
		this.neo4jAL = neo4jAL;
	}

}
