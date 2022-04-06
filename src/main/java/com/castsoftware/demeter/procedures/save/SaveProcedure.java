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

package com.castsoftware.demeter.procedures.save;

import com.castsoftware.demeter.controllers.state.StateController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.BooleanResult;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class SaveProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.save.levels", mode = Mode.WRITE)
    @Description(
            "demeter.save.levels(String ApplicationName, String SaveName) - Save the current state of an application.")
    public Stream<OutputMessage> saveLevels(
            @Name(value = "ApplicationName") String applicationName,
            @Name(value = "SaveName") String saveName)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            int count = StateController.saveDemeterLevel5(nal, applicationName, saveName);
            String msg =
                    String.format("%d Objects were saved in application %s.", count, applicationName);
            return Stream.of(new OutputMessage(msg));
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.save.getByApplication", mode = Mode.WRITE)
    @Description(
            "demeter.save.getByApplication(String ApplicationName) - Get all the saves related to a specific application.")
    public Stream<NodeResult> getSaveNodesByApplication(
            @Name(value = "ApplicationName") String applicationName) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            List<Node> nodeList = StateController.getSaveNodesByApplication(nal, applicationName);
            return nodeList.stream().map(NodeResult::new);
        } catch (Exception | Neo4jConnectionError | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    /**
     * Get all Demeter save present in the database
     *
     * @return
     * @throws ProcedureException
     */
    @Procedure(value = "demeter.save.getAll", mode = Mode.WRITE)
    @Description("demeter.save.getAll() - Get all Demeter save present in the database.")
    public Stream<NodeResult> getAllSaveNodes() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            List<Node> nodeList = StateController.getAllSaveNodes(nal);
            return nodeList.stream().map(NodeResult::new);
        } catch (Exception | Neo4jConnectionError | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    /**
     * Remove a Demeter 'save' state from the database.
     *
     * @param saveName Name of the save to remove
     * @return True if the operation is a success, False if the node was not found.
     * @throws ProcedureException
     */
    @Procedure(value = "demeter.save.removeSave", mode = Mode.WRITE)
    @Description(
            "demeter.save.removeSave(String SaveName) - Delete a specific save by its name in the database.")
    public Stream<BooleanResult> removeSave(@Name(value = "SaveName") String saveName)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            boolean found = StateController.removeSave(nal, saveName);
            return Stream.of(new BooleanResult(found));
        } catch (Exception
                | Neo4jConnectionError
                | Neo4jQueryException
                | Neo4jNoResult
                | Neo4jBadRequestException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.save.removeAll", mode = Mode.WRITE)
    @Description("demeter.save.removeAll() - Remove all the Demeter saves from the database.")
    public Stream<OutputMessage> removeAllSaves() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            int count = StateController.removeAllSaves(nal);
            String msg = String.format("%d save(s) were removed from the database.", count);
            return Stream.of(new OutputMessage(msg));
        } catch (Exception
                | Neo4jConnectionError
                | Neo4jQueryException
                | Neo4jNoResult
                | Neo4jBadRequestException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }
}
