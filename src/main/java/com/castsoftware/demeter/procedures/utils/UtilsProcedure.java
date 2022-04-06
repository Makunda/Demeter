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

package com.castsoftware.demeter.procedures.utils;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.controllers.UtilsController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class UtilsProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.clean", mode = Mode.WRITE)
    @Description("demeter.clean() - Clean the configuration tree")
    public void cleanConfiguration() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo("Starting Tagging clean..");

            UtilsController.deleteTaggingNodes(nal);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.removeTags", mode = Mode.WRITE)
    @Description("demeter.removeTags() - Clean the application from demeter tags.")
    public Stream<OutputMessage> removeTags() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo("Starting Tag cleaning..");

            int numAffected = UtilsController.removeTags(nal);

            return Stream.of(new OutputMessage(numAffected + " nodes were cleaned from Demeter Tags."));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.set.workspace", mode = Mode.WRITE)
    @Description("demeter.set.workspace - Change the default output directory.")
    public Stream<OutputMessage> setWorkspace(@Name(value = "OutputDirectory") String outputDir)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo("Changing the workspace to :" + outputDir);

            List<String> outputMessages = UtilsController.setWorkspace(outputDir);
            return outputMessages.stream().map(OutputMessage::new);

        } catch (Exception | Neo4jConnectionError | FileNotFoundException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.get.workspace", mode = Mode.WRITE)
    @Description("demeter.get.workspace - Get the actual value of the Workspace.")
    public Stream<OutputMessage> getWorkspace()
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            String actualWorkspace = UtilsController.getWorkspace();

            return Stream.of(new OutputMessage(actualWorkspace));

        } catch (Exception | Neo4jConnectionError | FileNotFoundException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.check", mode = Mode.WRITE)
    @Description("demeter.check() - Check if the provided requests are valid")
    public Stream<OutputMessage> healthCheck(
            @Name(value = "ApplicationContext") String applicationContext) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.logInfo("Starting health check..");
            String info = UtilsController.checkTags(nal, applicationContext);
            nal.logInfo(info);

            return Stream.of(new OutputMessage(info));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.version", mode = Mode.WRITE)
    @Description("demeter.version() - Get the version of the Extension")
    public Stream<OutputMessage> getVersion() throws ProcedureException {

        try {
            String version = Configuration.get("demeter.version");
            return Stream.of(new OutputMessage(version));
        } catch (Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

}
