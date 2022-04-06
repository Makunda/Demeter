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

package com.castsoftware.demeter.procedures.configuration;

import com.castsoftware.demeter.controllers.configuration.ConfigurationController;
import com.castsoftware.demeter.controllers.configuration.TagController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.demeter.TagNode;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConfigurationProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.createConfiguration", mode = Mode.WRITE)
    @Description("demeter.createConfiguration(String name) - Create a configuration node")
    public Stream<NodeResult> createConfiguration(@Name(value = "Name") String name)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            Node n = ConfigurationController.createConfiguration(nal, name);

            return Stream.of(new NodeResult(n));
        } catch (Neo4jBadRequestException | Neo4jNoResult | Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.forecast", mode = Mode.WRITE)
    @Description("demeter.forecast() - Get the number of request that will be executed")
    public Stream<OutputMessage> forecast(@Name(value = "Configuration") String configurationName)
            throws ProcedureException {
        List<Node> nodeList = new ArrayList<>();

        try {
            log.info("Launching forecast Procedure ..");
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            List<TagNode> lNode = TagController.getSelectedTags(nal, configurationName);

            int numReq = lNode.size();

            String message =
                    String.format("In this configuration %d request(s) will be executed.", numReq);
            return Stream.of(new OutputMessage(message));

        } catch (Neo4jConnectionError
                | Neo4jQueryException
                | Neo4jNoResult
                | Neo4jBadRequestException
                | Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.execute", mode = Mode.WRITE)
    @Description(
            "demeter.execute( String ConfigurationName, String Application ) - Execute a configuration node")
    public Stream<OutputMessage> executeConfiguration(
            @Name(value = "Configuration") String configurationName,
            @Name(value = "Application") String applicationLabel)
            throws ProcedureException {
        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            long start = System.currentTimeMillis();

            int numExec =
                    ConfigurationController.executeConfiguration(nal, configurationName, applicationLabel);

            long end = System.currentTimeMillis();
            long elapsedTime = end - start;

            String message =
                    String.format("%d demeter requests were executed in %d ms.", numExec, elapsedTime);
            return Stream.of(new OutputMessage(message));
        } catch (Neo4jBadRequestException
                | Neo4jNoResult
                | RuntimeException
                | Neo4jConnectionError
                | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }
}
