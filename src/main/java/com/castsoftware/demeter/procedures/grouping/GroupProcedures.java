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

package com.castsoftware.demeter.procedures.grouping;

import com.castsoftware.demeter.controllers.grouping.LevelGroupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.OutputMessage;
import com.castsoftware.demeter.utils.LevelsUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class GroupProcedures {


    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.group.levels", mode = Mode.WRITE)
    @Description("demeter.group.levels(String applicationName) - Group the levels following Demeter tags applied")
    public Stream<NodeResult> groupLevels(@Name(value = "ApplicationName") String applicationName) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            List<Node> nodes = LevelGroupController.groupAllLevels(nal, applicationName);

            return nodes.stream().map(NodeResult::new);

        } catch ( Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }


    @Procedure(value = "demeter.group.refresh.abstracts", mode = Mode.WRITE)
    @Description("demeter.group.refresh.abstracts(String applicationName) - Refresh the abstract level of your application")
    public Stream<OutputMessage> refreshAbstractLevels(@Name(value = "ApplicationName") String applicationName) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo("Starting abstract level refresh...");

            LevelsUtils.refreshAllAbstractLevel(nal, applicationName);

            nal.logInfo("Done !");

            return Stream.of(new OutputMessage("All the abstract levels were successfully refreshed"));

        } catch ( Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }

    }


}
