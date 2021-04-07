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

package com.castsoftware.demeter.procedures.grouping.architecture;

import com.castsoftware.demeter.controllers.grouping.GroupingUtilsController;
import com.castsoftware.demeter.controllers.grouping.architectures.ArchitectureGroupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.LongResult;
import com.castsoftware.demeter.results.NodeResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class ArchitectureProcedure {

	@Context public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;


	@Procedure(value = "demeter.group.architectures", mode = Mode.WRITE)
	@Description(
			"demeter.group.architectures(String applicationName) - Group the architectures following Demeter tags applied")
	public Stream<NodeResult> groupArchitectures(@Name(value = "ApplicationName") String applicationName)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			ArchitectureGroupController ag = new ArchitectureGroupController(nal, applicationName);
			List<Node> nodes = ag.launch();

			return nodes.stream().map(NodeResult::new);

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "demeter.delete.architectures.subset", mode = Mode.WRITE)
	@Description(
			"demeter.delete.architectures.subset(String applicationName, String Subset) - Delete a subset in a view")
	public Stream<LongResult> deleteSubSet(@Name(value = "ApplicationName") String applicationName,
										   @Name(value = "Subset") String subset)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			ArchitectureGroupController ag = new ArchitectureGroupController(nal, applicationName);
			Long numObj = ag.deleteSubModel(subset);

			return Stream.of(new LongResult(numObj));

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "demeter.delete.architectures.view", mode = Mode.WRITE)
	@Description(
			"demeter.delete.architectures.view(String applicationName, String architecture) - Delete a architects view")
	public Stream<LongResult> deleteArchitecture(@Name(value = "ApplicationName") String applicationName,
										   @Name(value = "architecture") String architecture)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			ArchitectureGroupController ag = new ArchitectureGroupController(nal, applicationName);
			Long numObj = ag.deleteArchi(architecture);

			return Stream.of(new LongResult(numObj));

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "demeter.api.group.architectures.views.all", mode = Mode.WRITE)
	@Description(
			"demeter.api.group.architectures.views.all() - Group all the architectures view")
	public Stream<NodeResult> groupInAllApplications()
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			List<Node> nodeList = GroupingUtilsController.groupAllArchitecture(nal);

			return nodeList.stream().map(NodeResult::new);

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "demeter.api.refresh.architecture", mode = Mode.WRITE)
	@Description(
			"demeter.api.refresh.architecture(String application, String architecture) - Refresh and recalculate the link of one architecture.")
	public void refreshArchitectureView(@Name(value = "ApplicationName") String applicationName,
							@Name(value = "architecture") String architectureName)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			GroupingUtilsController.refreshArchitecture(nal, applicationName, architectureName);

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}


}
