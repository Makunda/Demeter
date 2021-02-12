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

package com.castsoftware.demeter.procedures.api.grouping;

import com.castsoftware.demeter.controllers.api.GroupingController;
import com.castsoftware.demeter.controllers.grouping.architectures.ArchitectureGroupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.OutputMessage;
import com.castsoftware.demeter.results.demeter.CandidateFindingResult;
import com.castsoftware.demeter.results.demeter.DemeterGroupResult;
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
        } catch (Exception | MissingFileException | FileNotFoundException e) {
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
        } catch (Exception | MissingFileException | FileNotFoundException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.get.prefix.architecture", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.prefix.architecture() - Get the prefix of the architecture grouping")
    public Stream<OutputMessage> getArchiPrefix() throws ProcedureException {
        try {
            String prefix = ArchitectureGroupController.getPrefix();
            return Stream.of(new OutputMessage(prefix));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.set.prefix.architecture", mode = Mode.WRITE)
    @Description(
            "demeter.api.set.prefix.architecture(String newPrefix) - Set the prefix of the architecture grouping")
    public Stream<OutputMessage> setArchiPrefix(@Name(value = "Prefix") String prefix) throws ProcedureException {
        try {
            ArchitectureGroupController.setPrefix(prefix);
            return Stream.of(new OutputMessage(prefix));
        } catch (Exception | MissingFileException | FileNotFoundException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }



    @Procedure(value = "demeter.api.get.candidate.modules", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.candidate.modules(Optional String application) - Get the candidates for the module grouping")
    public Stream<CandidateFindingResult> getCandidateModuleGrouping(@Name(value="Application", defaultValue = "") String application) throws ProcedureException {
        try {
            Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);
            List<CandidateFindingResult> candidates;
            if(application.isEmpty()) {
                candidates = GroupingController.getCandidateApplicationsModuleGroup(neo4jAL);
            } else {
                candidates = GroupingController.getCandidateApplicationsModuleGroup(neo4jAL, application);
            }
            return candidates.stream();
        } catch (Exception | Neo4jQueryException | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }




    @Procedure(value = "demeter.api.get.demeter.modules", mode = Mode.WRITE)
    @Description(
            "demeter.api.get.demeter.modules(String application) - Get the levels grouped by demeter in one application")
    public Stream<DemeterGroupResult> getDemeterModules(@Name(value="Application") String application) throws ProcedureException {
        try {
            Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);
            List<DemeterGroupResult> levels = GroupingController.getDemeterModules(neo4jAL, application);

            return levels.stream();
        } catch (Exception | Neo4jQueryException | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }


}
