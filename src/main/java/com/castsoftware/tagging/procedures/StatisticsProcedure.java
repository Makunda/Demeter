package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.controllers.UtilsController;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class StatisticsProcedure {

    @org.neo4j.procedure.Context
    public GraphDatabaseService db;

    @org.neo4j.procedure.Context
    public Transaction transaction;

    @Context
    public Log log;

    /**
     * Extract the best candidates for Imaging value demo
     * @param configurationName
     * @param applicationLabel
     * @return
     * @throws ProcedureException
     */
    @Procedure(value = "tagging.statistics.highlight", mode = Mode.WRITE)
    @Description("tagging.statistics.highlight( String ConfigurationName, String Application ) - Execute a configuration node")
    public Stream<OutputMessage> executeConfiguration(@Name(value = "Configuration") String configurationName, @Name(value = "Application") String applicationLabel) throws ProcedureException {
        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            long start = System.currentTimeMillis();

            int numExec = UtilsController.executeConfiguration(nal, configurationName, applicationLabel);

            long end = System.currentTimeMillis();
            long elapsedTime = end - start;

            String message = String.format("%d tagging requests were executed in %d ms.", numExec, elapsedTime);
            return Stream.of(new OutputMessage(message));
        } catch (Neo4jBadRequestException | Neo4jNoResult | RuntimeException | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

}
