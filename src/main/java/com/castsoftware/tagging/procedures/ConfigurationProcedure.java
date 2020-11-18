package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.controllers.TagController;
import com.castsoftware.tagging.controllers.UtilsController;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.controllers.ConfigurationController;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.results.NodeResult;
import com.castsoftware.tagging.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConfigurationProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "tagging.createConfiguration", mode = Mode.WRITE)
    @Description("tagging.createConfiguration(String name) - Create a configuration node")
    public Stream<NodeResult> createConfiguration(@Name(value = "Name") String name) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            Node n = ConfigurationController.createConfiguration(nal, name);

            return Stream.of(new NodeResult(n));
        } catch (Neo4jBadRequestException | Neo4jNoResult | Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }

    @Procedure(value = "tagging.forecast", mode = Mode.WRITE)
    @Description("tagging.forecast() - Get the number of request that will be executed")
    public Stream<OutputMessage> forecast(@Name(value = "Configuration") String configurationName) throws ProcedureException {
        List<Node> nodeList = new ArrayList<>();

        try {
            log.info("Launching forecast Procedure ..");
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            List<TagNode> lNode = TagController.getSelectedTags(nal, configurationName);

            int numReq = lNode.size();

            String message = String.format("In this configuration %d request(s) will be executed.", numReq);
            return Stream.of(new OutputMessage(message));

        } catch (Neo4jConnectionError | Neo4jQueryException | Neo4jNoResult | Neo4jBadRequestException | Exception e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    @Procedure(value = "tagging.execute", mode = Mode.WRITE)
    @Description("tagging.execute( String ConfigurationName, String Application ) - Execute a configuration node")
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
