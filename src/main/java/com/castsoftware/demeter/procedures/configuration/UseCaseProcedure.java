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

package com.castsoftware.demeter.procedures.configuration;

import com.castsoftware.demeter.controllers.configuration.UseCaseController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.demeter.UseCaseNode;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.OutputMessage;
import com.castsoftware.demeter.results.UseCasesMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class UseCaseProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "demeter.useCases.add", mode = Mode.WRITE)
  @Description(
      "demeter.useCases.add( Long idParent, String name, Boolean active) - Add a use case to a configuration node or another usecase node.")
  public Stream<NodeResult> addUseCase(
      @Name(value = "idParent") Long idParent,
      @Name(value = "Name") String name,
      @Name(value = "Active", defaultValue = "False") Boolean active)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      nal.logInfo(
          String.format(
              "Adding a use case with parameters { 'Name' : '%s' , 'Active' : %b } ",
              name, active));

      Node n = UseCaseController.addUseCase(nal, name, active, idParent);

      nal.logInfo("Done !");

      return Stream.of(new NodeResult(n));

    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadRequestException
        | Neo4jNoResult e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.useCases.list", mode = Mode.WRITE)
  @Description("demeter.useCases.list() - List all the use cases present.")
  public Stream<UseCasesMessage> listUseCaseNodes() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      nal.logInfo("Starting Use Case Listing..");

      List<UseCaseNode> useCases = UseCaseController.listUseCases(nal);
      List<UseCasesMessage> messages = new ArrayList<>();
      for (UseCaseNode useCase : useCases) {
        messages.add(new UseCasesMessage(useCase));
      }

      return messages.stream();

    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadRequestException
        | Neo4jNoResult e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  /**
   * Activate a use case node and all its children.
   *
   * @param idUseCase Id of the use case
   * @param activation Value that will be set to all matching nodes.
   * @return The list of all node concerned by the modification
   * @throws ProcedureException
   */
  @Procedure(value = "demeter.useCases.activate", mode = Mode.WRITE)
  @Description(
      "demeter.useCases.activate(Long idUseCase, Boolean Activation) - Set the activation of the use case node and all other nodes under it.")
  public Stream<UseCasesMessage> activateUseCase(
      @Name(value = "Id") Long idUseCase, @Name(value = "Activation") Boolean activation)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<UseCaseNode> useCases = UseCaseController.selectUseCase(nal, idUseCase, activation);
      List<UseCasesMessage> messages = new ArrayList<>();

      for (UseCaseNode useCase : useCases) {
        messages.add(new UseCasesMessage(useCase));
      }

      return messages.stream();

    } catch (Exception
        | Neo4jConnectionError
        | Neo4jQueryException
        | Neo4jBadRequestException
        | Neo4jNoResult e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.useCases.globalActivation", mode = Mode.WRITE)
  @Description(
      "demeter.useCases.globalActivation(Boolean Activation) - Set the activation of every use case nodes.")
  public Stream<OutputMessage> globalActivationUseCase(
      @Name(value = "Activation") Boolean activation) throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      int nModifications = UseCaseController.activateAllUseCase(nal, activation);

      String message =
          String.format(
              "The activation parameter is now set to \"%b\" on %d nodes.",
              activation, nModifications);

      return Stream.of(new OutputMessage(message));

    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "demeter.useCases.globalSelection", mode = Mode.WRITE)
  @Description(
      "demeter.useCases.globalSelection(Boolean Activation) - Set the Selection of every use case nodes.")
  public Stream<OutputMessage> globalSelectionUseCase(
      @Name(value = "Activation") Boolean activation) throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      int nModifications = UseCaseController.selectAllUseCase(nal, activation);

      String message =
          String.format(
              "The selection parameter is now set to \"%b\" on %d nodes.",
              activation, nModifications);

      return Stream.of(new OutputMessage(message));

    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
