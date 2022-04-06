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

package com.castsoftware.demeter.procedures.grouping.aggregation;

import com.castsoftware.demeter.controllers.grouping.aggregations.AggregationController;
import com.castsoftware.demeter.controllers.grouping.levels.LevelGroupController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.NodeResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class AggregationProcedures {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.api.create.customNode", mode = Mode.WRITE)
    @Description(
            "demeter.api.create.customNode(String applicationName, Long aggregationID, String customName, List<Long> idNodes) " +
                    "- Create a custom node in the application and link it to the aggregation model ")
    public Stream<NodeResult> createCustomNode(@Name(value = "ApplicationName") String applicationName,
                                               @Name(value = "AggregationID") Long aggregationID,
                                               @Name(value = "CustomName") String customName,
                                               @Name(value = "IdNodes") List<Long> idNodes
    )
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            AggregationController agc = new AggregationController(nal, applicationName);
            Node node = agc.createCustom(aggregationID, customName, idNodes);
            NodeResult res = new NodeResult(node);

            return Stream.of(res);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure 'demeter.api.create.customNode'.", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.create.aggregation", mode = Mode.WRITE)
    @Description(
            "demeter.api.create.aggregation(String applicationName, String aggregationName) " +
                    "- Create a new aggregation view or merge it in the application ")
    public Stream<NodeResult> createAggregation(@Name(value = "ApplicationName") String applicationName,
                                                @Name(value = "AggregationName") String aggregationName)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            AggregationController agc = new AggregationController(nal, applicationName);
            Node node = agc.createAggregation(aggregationName);
            NodeResult res = new NodeResult(node);

            return Stream.of(res);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure 'demeter.api.create.aggregation'.", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.refresh.aggregation", mode = Mode.WRITE)
    @Description(
            "demeter.api.refresh.aggregation(String applicationName, Long AggregationId) " +
                    "- Refresh an aggregation view in the application ")
    public void refreshAggregation(@Name(value = "ApplicationName") String applicationName,
                                   @Name(value = "AggregationId") Long aggregationId)
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            AggregationController agc = new AggregationController(nal, applicationName);
            agc.refreshAggregation(aggregationId);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure 'demeter.api.refresh.aggregation'.", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.api.delete.aggregation", mode = Mode.WRITE)
    @Description(
            "demeter.api.delete.aggregation(String applicationName, String aggregationName, List<Long> idNodes) " +
                    "- Create a new aggregation view in the application ")
    public void deleteAggregation(@Name(value = "ApplicationName") String applicationName,
                                  @Name(value = "AggregationName") String aggregationName
    )
            throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            AggregationController agc = new AggregationController(nal, applicationName);
            agc.deleteAggregationByName(aggregationName);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure 'demeter.api.delete.aggregation'.", e);
            throw ex;
        }
    }
}
