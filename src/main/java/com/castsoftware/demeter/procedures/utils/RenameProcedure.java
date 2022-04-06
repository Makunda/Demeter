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

import com.castsoftware.demeter.controllers.configuration.RenameController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.BooleanResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;


public class RenameProcedure {

    // renameLevel
    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.rename.level", mode = Mode.WRITE)
    @Description("demeter.rename.level(String application, String oldName, String newName) - Rename a level in an application.")
    public Stream<BooleanResult> renameLevel(
            @Name(value = "ApplicationName") String applicationName,
            @Name(value = "OldName") String oldName,
            @Name(value = "NewName") String newName)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.logInfo("Starting Tagging export..");
            boolean changed = RenameController.renameLevel(nal, applicationName, oldName, newName);

            return Stream.of(new BooleanResult(changed));
        } catch (Exception | Neo4jConnectionError | Neo4jNoResult | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }
}
