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

import com.castsoftware.demeter.controllers.BackupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.*;
import com.castsoftware.demeter.results.NodeResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class BackupProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "demeter.undo.levels", mode = Mode.WRITE)
  @Description(
      "demeter.undo.levels(String ApplicationContext) - Add a tag node and link it to a use case node.")
  public Stream<NodeResult> undoLevels(
      @Name(value = "ApplicationContext") String applicationContext) throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);

      String message = "Undo level procedure launched ... ";
      nal.logInfo(message);

      List<Node> recoveredLevel = BackupController.undoLevelGroups(nal, applicationContext);
      String results = String.format("The procedure recreated %d level.", recoveredLevel.size());
      nal.logInfo(results);

      return recoveredLevel.stream().map(NodeResult::new);

    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadRequestException
        | Neo4jNoResult
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.undo.oneLevel", mode = Mode.WRITE)
  @Description(
      "demeter.undo.oneLevel(String ApplicationContext, String levelName) - Add a tag node and link it to a use case node.")
  public Stream<NodeResult> undoOneLevelGroup(
      @Name(value = "ApplicationContext") String applicationContext,
      @Name(value = "LevelName") String levelName)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);

      String message = "Undoing one level procedure launched ... ";
      nal.logInfo(message);

      List<Node> recoveredLevel =
          BackupController.undoOneLevelGroup(nal, applicationContext, levelName);
      String results = String.format("The procedure recreated level %s.", recoveredLevel.size());
      nal.logInfo(results);

      return recoveredLevel.stream().map(NodeResult::new);

    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadRequestException
        | Neo4jNoResult
        | Neo4jBadNodeFormatException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
