package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;

import com.castsoftware.tagging.controllers.UtilsController;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

public class UtilsProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    @Procedure(value = "tagging.export", mode = Mode.WRITE)
    @Description("tagging.export() - Clean the configuration tree")
    public void exportConfiguration(@Name(value = "Path") String path, @Name(value= "Filename") String filename ) throws ProcedureException {

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction
            nal.info("Starting Tagging clean..");

            UtilsController.exportConfiguration(nal, path, filename);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();
        } catch (Exception | Neo4jConnectionError | com.castsoftware.exporter.exceptions.ProcedureException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }

    @Procedure(value = "tagging.import", mode = Mode.WRITE)
    @Description("tagging.import() - Clean the configuration tree")
    public void importConfiguration(@Name(value = "Path") String path ) throws ProcedureException {

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction
            nal.info("Starting Tagging clean..");

            UtilsController.importConfiguration(nal, path);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();
        } catch (Exception | Neo4jConnectionError | com.castsoftware.exporter.exceptions.ProcedureException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }


    @Procedure(value = "tagging.clean", mode = Mode.WRITE)
    @Description("tagging.clean() - Clean the configuration tree")
    public void cleanConfiguration() throws ProcedureException {

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction
            nal.info("Starting Tagging clean..");

            UtilsController.deleteTaggingNodes(nal);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }
}
