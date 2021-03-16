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

import com.castsoftware.demeter.controllers.grouping.architectures.ArchitectureGroupController;
import com.castsoftware.demeter.controllers.grouping.architectures.MicroserviceController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class MicroservicesProcedures {

	@Context
	public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;


	@Procedure(value = "demeter.extract.microservice", mode = Mode.WRITE)
	@Description(
			"demeter.extract.microservice(String applicationName, String prefix) - Extract a part of the application to a whole new microservice")
	public Stream<OutputMessage> extractMicroservice(@Name(value = "ApplicationName") String applicationName, @Name(value = "Prefix") String prefix)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			MicroserviceController mc = new MicroserviceController(nal, applicationName);

			mc.extractMicroservice(prefix);
			return Stream.of(new OutputMessage("OK"));

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "demeter.extract.one.microservice", mode = Mode.WRITE)
	@Description(
			"demeter.extract.one.microservice(String applicationName, String prefix, Long idStart) - Extract a part of the application starting with a node")
	public Stream<OutputMessage> extractOneMicroservice(@Name(value = "ApplicationName") String applicationName, @Name(value = "Prefix") String prefix,  @Name(value = "IdStart") Long idStart)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			MicroserviceController mc = new MicroserviceController(nal, applicationName);

			mc.extractOneMicroservice(prefix, idStart);
			return Stream.of(new OutputMessage("OK"));

		} catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jBadNodeFormatException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

}
