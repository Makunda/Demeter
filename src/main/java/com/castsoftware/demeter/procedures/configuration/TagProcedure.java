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

import com.castsoftware.demeter.controllers.configuration.TagController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.*;
import com.castsoftware.demeter.models.demeter.TagNode;
import com.castsoftware.demeter.results.BooleanResult;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.demeter.TagResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class TagProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.tag.add", mode = Mode.WRITE)
    @Description("demeter.tag.add(String Tag, String AssociatedRequest, Boolean Activation, String Description, String categories, Long ParentId) - Add a tag node and link it to a use case node.")
    public Stream<NodeResult> addTagNode(@Name(value = "Tag") String tag,
                                   @Name(value= "AssociatedRequest")  String associatedRequest,
                                   @Name(value= "Activation") Boolean activation,
                                   @Name(value= "Description")  String description,
                                   @Name(value= "Categories")  String categories,
                                   @Name(value= "ParentId")  Long parentId) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            String message = String.format("Adding a %s node with parameters { 'Tag' : '%s', 'Activation' : %s, 'Request' : '%s' }.", TagNode.getLabel(), tag, activation, associatedRequest);
            nal.logInfo(message);

            Node n =  TagController.addTagNode(nal, tag, activation, associatedRequest, description, categories,  parentId);
            return Stream.of(new NodeResult(n));
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.tag.getAsList", mode = Mode.WRITE)
    @Description("demeter.tag.getAsList( String ConfigurationName, String ApplicationName ) - Get the result of the Tag on a specific application as a List.")
    public Stream<TagResult> getTagResultsAsList(@Name(value = "ConfigurationName") String configurationName,
                                          @Name(value= "ApplicationName")  String applicationName) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            List<TagResult> resultList =  TagController.forecastTag(nal, configurationName, applicationName);
            return resultList.stream();
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.tag.execute", mode = Mode.WRITE)
    @Description("demeter.tag.execute( Long id, String ApplicationName ) - Get the result of the Tag on a specific application as a List.")
    public Stream<TagResult> executeTag(@Name(value = "Id") Long tagId,
                                        @Name(value= "ApplicationName")  String applicationName) throws ProcedureException {
        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            TagResult result =  TagController.executeTag(nal, tagId, applicationName);
            return Stream.of(result);
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jNoResult | Neo4jBadNodeFormatException e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.tag.validateQuery", mode = Mode.WRITE)
    @Description("demeter.tag.validateQuery( String queryToValidate ) - Return the validity of the request.")
    public Stream<BooleanResult> validateQuery(@Name(value= "Request")  String request) throws ProcedureException {
        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            boolean result =  TagController.validateQuery(nal, request);
            return Stream.of(new BooleanResult(result));
        } catch (Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

}
