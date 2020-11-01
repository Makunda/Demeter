package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.controllers.ConfigurationController;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    @Procedure(value = "tagging.createConfiguration", mode = Mode.WRITE)
    @Description("tagging.createConfiguration(String name) - Create a configuration node")
    public void createConfiguration(@Name(value = "Name") String name) throws ProcedureException {
        List<Node> nodeList = new ArrayList<>();

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction

            Node n = ConfigurationController.createConfiguration(nal, name);
            nodeList.add(n);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();
        } catch (Neo4jBadRequest | Neo4jNoResult | Exception | Neo4jConnectionError e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }



}
