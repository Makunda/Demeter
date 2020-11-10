package com.castsoftware.tagging.procedures;

import com.castsoftware.exporter.results.OutputMessage;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;

import com.castsoftware.tagging.controllers.UtilsController;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class UtilsProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "tagging.export", mode = Mode.WRITE)
    @Description("tagging.export() - Clean the configuration tree")
    public Stream<OutputMessage> exportConfiguration(@Name(value = "Path") String path, @Name(value= "Filename") String filename ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.info("Starting Tagging export..");

            return UtilsController.exportConfiguration(nal, path, filename);
        } catch (Exception | Neo4jConnectionError | com.castsoftware.exporter.exceptions.ProcedureException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }

    @Procedure(value = "tagging.import", mode = Mode.WRITE)
    @Description("tagging.import() - Clean the configuration tree")
    public Stream<OutputMessage> importConfiguration(@Name(value = "Path") String path ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.info("Starting Tagging import..");

            return UtilsController.importConfiguration(nal, path);
        } catch (Exception | Neo4jConnectionError | com.castsoftware.exporter.exceptions.ProcedureException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }


    @Procedure(value = "tagging.clean", mode = Mode.WRITE)
    @Description("tagging.clean() - Clean the configuration tree")
    public void cleanConfiguration() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.info("Starting Tagging clean..");

            UtilsController.deleteTaggingNodes(nal);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    @Procedure(value = "tagging.check", mode = Mode.WRITE)
    @Description("tagging.check() - Check if the provided requests are valid")
    public Stream<OutputMessage> healthCheck(@Name(value = "ApplicationContext") String applicationContext ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            nal.info("Starting health check..");
            String info =  UtilsController.checkTags(nal, applicationContext);
            nal.info(info);

            return Stream.of(new OutputMessage(info));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }
}
