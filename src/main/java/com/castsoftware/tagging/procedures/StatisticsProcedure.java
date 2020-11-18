package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.controllers.StatisticsController;
import com.castsoftware.tagging.controllers.UtilsController;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.StatisticNode;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.results.NodeResult;
import com.castsoftware.tagging.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.io.IOException;
import java.util.MissingFormatArgumentException;
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
     * @param applicationLabel
     * @return
     * @throws ProcedureException
     */
    @Procedure(value = "tagging.statistics.highlights", mode = Mode.WRITE)
    @Description("tagging.statistics.highlights( String ConfigurationName, String Application ) - Generate a pre-tagging statistics report.")
    public Stream<OutputMessage> findHighlights(@Name(value = "Configuration") String configurationName , @Name(value = "Application") String applicationLabel) throws ProcedureException {
        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            long start = System.currentTimeMillis();

            StatisticsController.writePreExecutionStatistics(nal, configurationName, applicationLabel);

            long end = System.currentTimeMillis();
            long elapsedTime = end - start;

            String message = String.format("Report generated in %d ms.", elapsedTime);
            return Stream.of(new OutputMessage(message));
        } catch (Neo4jBadRequestException | Neo4jConnectionError | Exception | Neo4jQueryException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            log.error("An error occurred during the execution of the request.", e);
            throw ex;
        }
    }

    @Procedure(value = "tagging.statistics.add", mode = Mode.WRITE)
    @Description("tagging.statistics.add(String Name, String request, Boolean Activation, String Description, Long ConfigurationId) - Add a Statistic node and link it to a use configuration node.")
    public Stream<NodeResult> addTagNode(@Name(value = "Name") String name,
                                         @Name(value= "Request") String  request,
                                         @Name(value= "Active")  Boolean active,
                                         @Name(value= "Description")  String description,
                                         @Name(value= "ParentId")  Long parentId) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            String message = String.format("Adding a %s node with parameters { 'Name' : '%s', 'Request' : '%s', 'Activation' : '%b', 'Description' : '%s' }.", StatisticNode.getLabel(), name, request, active, description);
            nal.logInfo(message);

            Node n =  StatisticsController.addStatisticNode(nal, name, request, active, description, parentId);
            return Stream.of(new NodeResult(n));
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

}
