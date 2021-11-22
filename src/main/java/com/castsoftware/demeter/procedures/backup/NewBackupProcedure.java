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

package com.castsoftware.demeter.procedures.backup;

import com.castsoftware.demeter.controllers.backup.NewBackupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class NewBackupProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "demeter.backup.application", mode = Mode.WRITE)
  @Description(
      "demeter.backup.application(String application, String name) - Save the actual state of the application")
  public Stream<OutputMessage> backupApplication(
      @Name(value = "Application", defaultValue = "") String application,
      @Name(value = "Save", defaultValue = "") String save)
      throws ProcedureException {
    try {
      // Check arguments
      if (save == null || save.isBlank())
        throw new Exception("The 'save' parameter must not be empty.");

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      // Backup
      NewBackupController controller = new NewBackupController(neo4jAL, application);
      controller.saveState(save);

      // Send message to user
      return Stream.of(new OutputMessage(String.format("The application '%s' has been saved. Check the logs for more information.", application)));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to save the application...", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.backup.rollback", mode = Mode.WRITE)
  @Description(
      "demeter.backup.rollback(String application, String name) - Save the actual state of the application")
  public Stream<OutputMessage> backupRollback(
      @Name(value = "Application", defaultValue = "") String application,
      @Name(value = "Save", defaultValue = "") String save)
      throws ProcedureException {
    try {
      // Check arguments
      if (save == null || save.isBlank())
        throw new Exception("The 'save' parameter must not be empty.");

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      // Rollback to previous state
      NewBackupController controller = new NewBackupController(neo4jAL, application);
      controller.rollBackToSave(save);

      // Stream the results
      return Stream.of(new OutputMessage(String.format("The application '%s' has been rollbacked. Check the logs for more information.", application)));

    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to rollback the application...", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.backup.get.list", mode = Mode.WRITE)
  @Description(
      "demeter.backup.get.list(String application) - Get the list of all saves in one application")
  public Stream<OutputMessage> getSaves(
      @Name(value = "Application", defaultValue = "") String application)
      throws ProcedureException {
    try {
      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      NewBackupController controller = new NewBackupController(neo4jAL, application);
      List<String> saves = controller.getListSave();
      return saves.stream().map(OutputMessage::new);
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to get the list of application's saves...", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.backup.delete", mode = Mode.WRITE)
  @Description(
      "demeter.backup.delete(String application, String name) - Save the actual state of the application")
  public Stream<OutputMessage> deleteSave(
      @Name(value = "Application", defaultValue = "") String application,
      @Name(value = "Save", defaultValue = "") String save)
      throws ProcedureException {
    try {
      // Check arguments
      if (save == null || save.isBlank())
        throw new Exception("The 'save' parameter must not be empty.");

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      NewBackupController controller = new NewBackupController(neo4jAL, application);
      controller.deleteSave(save);
      return Stream.of(
          new OutputMessage(String.format("Save '%s' has been successfully deleted in application '%s'.", save, application)));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to delete a save in the application...", e);
      throw ex;
    }
  }
}
