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

package com.castsoftware.demeter.procedures.grouping.module;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.controllers.grouping.AGrouping;
import com.castsoftware.demeter.controllers.grouping.GroupingUtilsController;
import com.castsoftware.demeter.controllers.grouping.modules.ModuleGroupController;
import com.castsoftware.demeter.controllers.imaging.ModuleController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
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


public class ModuleProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "demeter.module.get.hiddenLabel", mode = Mode.WRITE)
  @Description(
          "demeter.module.get.hiddenLabel() - Get the hidden label")
  public Stream<OutputMessage> getHiddenLabel()
          throws ProcedureException {
      try {
         return Stream.of(new OutputMessage(ModuleController.getHiddenPrefix()));
      } catch (Exception e) {
        ProcedureException ex = new ProcedureException(e);
        log.error("An error occurred while executing the procedure", e);
        throw ex;
      }
  }

  @Procedure(value = "demeter.module.hide.byId", mode = Mode.WRITE)
  @Description(
          "demeter.module.hide.byId(Long ModuleId) - Hide a specific label")
  public void hideModule(@Name(value = "ModuleId") Long moduleID)
          throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      ModuleController mc = new ModuleController(nal);
      mc.hideModuleById(moduleID);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.module.display.byId", mode = Mode.WRITE)
  @Description(
          "demeter.module.display.byId(Long ModuleId) - Display a specific label")
  public void displayModule(@Name(value = "ModuleId") Long moduleID)
          throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      ModuleController mc = new ModuleController(nal);
      mc.displayModuleById(moduleID);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.module.delete.byId", mode = Mode.WRITE)
  @Description(
          "demeter.module.delete.byId(Long ModuleId) - Display a specific label")
  public void deleteModule(@Name(value = "ModuleId") Long moduleID)
          throws ProcedureException {
    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      ModuleController mc = new ModuleController(nal);
      mc.deleteModule(moduleID);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.group.modules", mode = Mode.WRITE)
  @Description(
      "demeter.group.modules(String applicationName) - Group the modules following Demeter tags applied")
  public Stream<NodeResult> groupModules(@Name(value = "ApplicationName") String applicationName)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      ModuleGroupController mgc = new ModuleGroupController(nal, applicationName);
      List<Node> nodes = mgc.launch();

      return nodes.stream().map(NodeResult::new);

    } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.api.group.modules.all", mode = Mode.WRITE)
  @Description(
          "demeter.api.group.modules.all() - Group the modules following Demeter tags applied")
  public Stream<NodeResult> groupAll()
          throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<Node> nodes = GroupingUtilsController.groupAllModules(nal);
      return nodes.stream().map(NodeResult::new);

    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
