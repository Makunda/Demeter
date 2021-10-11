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

package com.castsoftware.demeter.database;

import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Neo4jAL {

  private static final String ERROR_PREFIX = "NEOALx";
  private static final String DEMETER_LOG_PREFIX = "Demeter :: ";

  private final Log log;
  private final GraphDatabaseService db;

  private Transaction transaction = null;
  private Boolean activeTransaction = false;

  /**
   * Constructor for the Neo4j Layer
   *
   * @param transaction
   * @param log
   * @throws Neo4jConnectionError If the database failed to respond in more than 15 seconds
   */
  public Neo4jAL(GraphDatabaseService db, Transaction transaction, Log log)
      throws Neo4jConnectionError {
    this.db = db;
    this.log = log;
    this.transaction = transaction;
    this.activeTransaction = true;
  }

  /**
   * Create an index on the desired property
   *
   * @param label Label concerned by the index
   * @param property Property used for the index
   * @throws Neo4jBadRequestException is thrown if the request contains an error, or if the
   *     execution failed.
   */
  private void setIndex(String label, String property) throws Neo4jBadRequestException {
    String indexQuery = String.format("CREATE INDEX ON:%s(%s)", label, property);
    try {
      this.executeQuery(indexQuery);
    } catch (Neo4jQueryException e) {
      throw new Neo4jBadRequestException(
          "Cannot set the index.", indexQuery, e, ERROR_PREFIX + "SETI1");
    }
  }

  /**
   * Find nodes using their Label
   *
   * @param label Label to search
   * @return <code>ResourceIterator</code> Iterator on the nodes found
   * @throws Neo4jQueryException Threw if the request produced an error
   */
  public ResourceIterator<Node> findNodes(Label label) throws Neo4jQueryException {
    try {
      return this.transaction.findNodes(label);
    } catch (Exception e) {
      throw new Neo4jQueryException(
          String.format("Cannot find all nodes with label '%s'", label.toString()),
          e,
          ERROR_PREFIX + "FIND1");
    }
  }

  /**
   * Delete a node by its ID
   * @param idNode Id of the node to delete
   * @throws Neo4jQueryException Exception during the execution of the query
   */
  public void deleteNodeById(Long idNode) throws Neo4jQueryException {
    String req = "MATCH (o) WHERE ID(o)=$idNode DETACH DELETE o;";
    Map<String, Object> params = Map.of("idNode", idNode);

    try {
      this.executeQuery(req, params);
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
              "Error while deleting a node.", req, e, ERROR_PREFIX + "DELN1");
    }
  }


  /**
   * Delete a node by its ID
   * @param idNode Id of the node to delete
   * @throws Neo4jQueryException Exception during the execution of the query
   */
  public void deleteByIdAndLabel(Long idNode, String label) throws Neo4jQueryException {
    String req = String.format("MATCH (o:`%s`) WHERE ID(o)=$idNode DETACH DELETE o;", label);
    Map<String, Object> params = Map.of("idNode", idNode);

    try {
      this.executeQuery(req, params);
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
              "Error while deleting a node.", req, e, ERROR_PREFIX + "DELN1");
    }
  }


  /**
   * Execute a single query
   *
   * @param query Cypher query to execute
   * @return Result of the cypher query
   * @throws Neo4jQueryException Exception during the processing of the query
   */
  public Result executeQuery(String query) throws Neo4jQueryException {
    try {
      return this.transaction.execute(query);
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
          "Error while executing query.", query, e, ERROR_PREFIX + "EXQS1");
    }
  }

  /**
   * Execute a single query with associated parameters
   *
   * @param query Cypher query to execute
   * @param params Parameters of the query
   * @return Result of the cypher query
   * @throws Neo4jQueryException Exception during the processing of the query
   */
  public Result executeQuery(String query, Map<String, Object> params) throws Neo4jQueryException {
    try {
      return this.transaction.execute(query, params);
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
          "Error while executing query with parameters.", query, e, ERROR_PREFIX + "EXQS1");
    }
  }

  public Result executeAtomicQuery(String query, Map<String, Object> params)
      throws Neo4jQueryException {
    try (Transaction tx = db.beginTx()) {
      return tx.execute(query, params);
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
          "Error while executing query with parameters.", query, e, ERROR_PREFIX + "EXQS1");
    }
  }

  public Result executeAtomicQuery(String query) throws Neo4jQueryException {
    try (Transaction tx = db.beginTx()) {
      return tx.execute(query);
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
          "Error while executing query with parameters.", query, e, ERROR_PREFIX + "EXQS1");
    }
  }

  /**
   * Execute a list of query. Commit only if all query were executed without errors. Rollback
   * otherwise.
   *
   * @param queries List of cypher query to execute
   * @return List of Result
   * @throws Neo4jQueryException Exception during the processing of one of the query
   */
  public List<Result> executeQuery(List<String> queries) throws Neo4jQueryException {

    List<Result> results = new ArrayList<>();
    try {
      for (String q : queries) {
        Result qResults = this.transaction.execute(q);
        results.add(qResults);
      }
      return results;
    } catch (Exception e) {
      throw new Neo4jQueryException("Cannot execute multiple queries", e, ERROR_PREFIX + "EXQS1");
    }
  }

  /**
   * Retriece a node using its ID
   *
   * @param id ID of the node
   * @return <code>Node</code> if the node if found, null otherwise
   * @throws Neo4jConnectionError
   * @throws Neo4jQueryException
   */
  public Node getNodeById(Long id) throws Neo4jQueryException {

    try {
      return this.transaction.getNodeById(id);
    } catch (NotFoundException e) {
      return null;
    } catch (Exception e) {
      throw new Neo4jQueryException("Cannot execute multiple queries", e, ERROR_PREFIX + "GNBI2");
    }
  }

  /**
   * Delete all the nodes matching the provided label.
   *
   * @param label Label to delete
   * @return <code>Integer</code> number of node deleted.
   * @throws Neo4jQueryException
   */
  public int deleteAllNodesByLabel(Label label) throws Neo4jQueryException {
    try {
      int numbDeleted = 0;

      for (ResourceIterator<Node> it = this.transaction.findNodes(label); it.hasNext(); ) {
        Node n = it.next();

        // Detach all relationships
        for (Relationship r : n.getRelationships()) {
          r.delete();
        }
        // Delete the node
        n.delete();
        numbDeleted++;
      }

      return numbDeleted;
    } catch (QueryExecutionException e) {
      throw new Neo4jQueryException(
          "Error while executing query.", "Delete all nodes by label", e, ERROR_PREFIX + "DELL1");
    }
  }

  /**
   * Get all labels
   *
   * @return return all the labels present in the database as a list.
   */
  public List<Label> getAllLabels() {
    List<Label> labels = new ArrayList<>();

    for (Label l : this.transaction.getAllLabels()) {
      labels.add(l);
    }

    return labels;
  }

  public Log getLogger() {
    return log;
  }

  public GraphDatabaseService getDb() {
    return this.db;
  }

  /**
   * Commit the transaction. If the transaction is closed before being committed, it will be
   * rolledback. DO NOT USE WHEN DEALING WITH SESSION TRANSACTION
   *
   * @deprecated
   */
  public void commitTransaction() {
    this.transaction.commit();
  }

  public void rollbackTransaction() {
    this.transaction.rollback();
  }

  public Transaction getTransaction() {
    return this.transaction;
  }

  public Boolean isOpen() {
    return this.activeTransaction;
  }

  public void logInfo(String message) {
    log.info(DEMETER_LOG_PREFIX + message);
  }

  public void logError(String message) {
    log.error(DEMETER_LOG_PREFIX + message);
  }

  public void logError(String message, Throwable e) {
    log.error(DEMETER_LOG_PREFIX + message, e);
  }
}
