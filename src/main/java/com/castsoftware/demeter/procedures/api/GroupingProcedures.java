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

package com.castsoftware.demeter.procedures.api;

import com.castsoftware.demeter.controllers.api.GroupingController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.OutputMessage;
import com.castsoftware.demeter.results.demeter.GroupingResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class GroupingProcedures {

    @Context
    public GraphDatabaseService db;

    @Context public Transaction transaction;

    @Context public Log log;

    @Procedure(value = "demeter.api.get.prefix.level", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.prefix.level() - Get the prefix of the level grouping")
    public Stream<OutputMessage> getLevelPrefix() throws ProcedureException {
        try {
            String prefix = GroupingController.getLevelGroupPrefix();
            return Stream.of(new OutputMessage(prefix));
        } catch (Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.set.prefix.level", mode = Mode.WRITE)
    @Description(
            "demeter.api.set.prefix.level(String newPrefix) - Set the prefix of the level grouping")
    public Stream<OutputMessage> setLevelPrefix(@Name(value = "Prefix") String prefix) throws ProcedureException {
        try {
            String newPrefix = GroupingController.setLevelGroupPrefix(prefix);
            return Stream.of(new OutputMessage(newPrefix));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.get.prefix.module", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.prefix.module() - Get the prefix of the module grouping")
    public Stream<OutputMessage> getModulePrefix() throws ProcedureException {
        try {
            String prefix = GroupingController.getModuleGroupPrefix();
            return Stream.of(new OutputMessage(prefix));
        } catch (Exception e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.set.prefix.module", mode = Mode.WRITE)
    @Description(
            "demeter.api.set.prefix.module(String newPrefix) - Set the prefix of the module grouping")
    public Stream<OutputMessage> setModulePrefix(@Name(value = "Prefix") String prefix) throws ProcedureException {
        try {
            String newPrefix = GroupingController.setModuleGroupPrefix(prefix);
            return Stream.of(new OutputMessage(newPrefix));
        } catch (Exception | MissingFileException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    // Group candidates


    @Procedure(value = "demeter.api.get.candidates.level", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.candidates.level() - Get the prefix of the level grouping")
    public Stream<GroupingResult> setCandidateLevelGrouping() throws ProcedureException {
        try {
            Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);
            List<GroupingResult> candidates = GroupingController.getCandidateApplicationsLevelGroup(neo4jAL);
            return candidates.stream();
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.get.candidates.module", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.candidates.module() - Get the prefix of the level grouping")
    public Stream<GroupingResult> setCandidateModuleGrouping() throws ProcedureException {
        try {
            Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);
            List<GroupingResult> candidates = GroupingController.getCandidateApplicationsModuleGroup(neo4jAL);
            return candidates.stream();
        } catch (Exception | Neo4jQueryException | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

}
