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

package com.castsoftware.demeter.procedures.metaModel;

import com.castsoftware.demeter.controllers.BackupController;
import com.castsoftware.demeter.controllers.configuration.MetaModelController;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.ProcedureException;
import com.castsoftware.demeter.exceptions.neo4j.*;
import com.castsoftware.demeter.results.NodeResult;
import com.castsoftware.demeter.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class MetaModelProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "demeter.metamodel.generate", mode = Mode.WRITE)
    @Description("demeter.metamodel.generate(String OutputDirectory) - Generate a blank metamodel template at the specified path.")
    public Stream<OutputMessage> generateTemplate(@Name(value = "OutputDirectory") String ouputdir) throws ProcedureException {

        try {
            String msg = MetaModelController.generateTemplate(ouputdir);
            return Stream.of(new OutputMessage(msg));
        } catch (Exception  e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }

    @Procedure(value = "demeter.metamodel.execute", mode = Mode.WRITE)
    @Description("demeter.metamodel.execute(String ApplicationContext, String MetaModelName) - Generate a blank metamodel template at the specified path.")
    public Stream<OutputMessage> executeMetamodel(@Name(value = "ApplicationContext") String applicationContext,
                                                  @Name(value = "MetaModelName") String metaModelName) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            long start = System.currentTimeMillis();

            MetaModelController.executeMetamodel(nal, applicationContext, metaModelName);

            long end = System.currentTimeMillis();
            long elapsedTime = end - start;

            return Stream.of(new OutputMessage(String.format("The metamodel was executed in %d milliseconds.", elapsedTime)));
        } catch (Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            log.error("An error occurred while executing the procedure", e);
            throw ex;
        }
    }


}
