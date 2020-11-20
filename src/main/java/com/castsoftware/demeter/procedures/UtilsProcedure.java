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

package com.castsoftware.demeter.procedures;

import com.castsoftware.exporter.results.OutputMessage;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;

import com.castsoftware.demeter.controllers.UtilsController;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class UtilsProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "tagging.export", mode = Mode.WRITE)
    @Description("tagging.export() - Clean the configuration tree")
    public Stream<OutputMessage> exportConfiguration(@Name(value = "Path") String path, @Name(value= "Filename") String filename ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.logInfo("Starting Tagging export..");

            return UtilsController.exportConfiguration(nal, path, filename);
        } catch (Exception | Neo4jConnectionError | com.castsoftware.exporter.exceptions.ProcedureException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }

    @Procedure(value = "tagging.import", mode = Mode.WRITE)
    @Description("tagging.import() - Clean the configuration tree")
    public Stream<OutputMessage> importConfiguration(@Name(value = "Path") String path ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.logInfo("Starting Tagging import..");

            return UtilsController.importConfiguration(nal, path);
        } catch (Exception | Neo4jConnectionError | com.castsoftware.exporter.exceptions.ProcedureException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }


    @Procedure(value = "tagging.clean", mode = Mode.WRITE)
    @Description("tagging.clean() - Clean the configuration tree")
    public void cleanConfiguration() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo("Starting Tagging clean..");

            UtilsController.deleteTaggingNodes(nal);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    @Procedure(value = "tagging.check", mode = Mode.WRITE)
    @Description("tagging.check() - Check if the provided requests are valid")
    public Stream<OutputMessage> healthCheck(@Name(value = "ApplicationContext") String applicationContext ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.logInfo("Starting health check..");
            String info =  UtilsController.checkTags(nal, applicationContext);
            nal.logInfo(info);

            return Stream.of(new OutputMessage(info));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }
}
