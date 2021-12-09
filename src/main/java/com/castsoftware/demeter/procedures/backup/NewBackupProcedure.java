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
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.backup.MasterSaveNode;
import com.castsoftware.demeter.results.OutputMessage;
import com.castsoftware.demeter.results.backup.MasterSaveResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class NewBackupProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "demeter.backup.application", mode = Mode.WRITE)
  @Description(
      "demeter.backup.application(String application, String name, String description, Long timestamp, String picture) - Save the actual state of the application")
  public Stream<OutputMessage> backupApplication(
      @Name(value = "Application") String application,
      @Name(value = "Name") String name,
      @Name(value = "Description", defaultValue = "") String description,
      @Name(value = "Timestamp", defaultValue = "0") Long timestamp,
      @Name(value = "Picture", defaultValue = "") String picture
  )
      throws ProcedureException {
    try {
      // Check arguments
      if (name == null || name.isBlank())
        throw new Exception("The 'Name' parameter must not be empty.");

      if (application == null || application.isBlank())
        throw new Exception("The 'Application' parameter must not be empty.");

      if(timestamp >= 0L) {
        timestamp = new Date().getTime();
      }

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      // Backup
      NewBackupController controller = new NewBackupController(neo4jAL, application);
      controller.saveState(name, description, timestamp, picture);

      // Send message to user
      return Stream.of(new OutputMessage(String.format("The application '%s' has been saved. Check the logs for more information.", application)));
    } catch (Exception | Neo4jConnectionError | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to save the application...", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.backup.rollback", mode = Mode.WRITE)
  @Description(
      "demeter.backup.rollback(String application, Long id) - Rollback the application to another save state")
  public Stream<OutputMessage> backupRollback(
      @Name(value = "Application") String application,
      @Name(value = "Id") Long id)
      throws ProcedureException {
    try {
      // Check arguments
      if (application == null || application.isBlank())
        throw new Exception("The 'Application' parameter must not be empty.");

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      // Rollback to previous state
      NewBackupController controller = new NewBackupController(neo4jAL, application);
      controller.rollBackToSave(id);

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
  public Stream<MasterSaveResult> getSaves(
      @Name(value = "Application", defaultValue = "") String application)
      throws ProcedureException {
    try {
      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      NewBackupController controller = new NewBackupController(neo4jAL, application);
      List<MasterSaveNode> saves = controller.getListSave();
      return saves.stream().map(MasterSaveResult::new);
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to get the list of application's saves...", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.backup.delete", mode = Mode.WRITE)
  @Description(
      "demeter.backup.delete(String application, Long id) - Delete a backup from the database")
  public Stream<OutputMessage> deleteSave(
      @Name(value = "Application") String application,
      @Name(value = "Id") Long id)
      throws ProcedureException {
    try {
      // Check arguments
      if (application == null || application.isBlank())
        throw new Exception("The 'Application' parameter must not be empty.");

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      NewBackupController controller = new NewBackupController(neo4jAL, application);
      controller.deleteSave(id);
      return Stream.of(
          new OutputMessage(String.format("Backup with id '%d' has been successfully deleted in application '%s'.", id, application)));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to delete a save in the application...", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.backup.download", mode = Mode.WRITE)
  @Description(
          "demeter.backup.download(String application, String path) - Download the complete schema of an application")
  public Stream<OutputMessage> download(
          @Name(value = "Application") String application,
          @Name(value = "Path") String path)
          throws ProcedureException {
    try {
      // Check arguments
      if (application == null || application.isBlank())
        throw new Exception("The 'Application' parameter must not be empty.");

      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      NewBackupController controller = new NewBackupController(neo4jAL, application);

      return Stream.of(
              new OutputMessage(String.format("Application '%s' has been exported to '%s' .", application,  path)));
    } catch (Exception | Neo4jConnectionError e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("Failed to delete a save in the application...", e);
      throw ex;
    }
  }
}
