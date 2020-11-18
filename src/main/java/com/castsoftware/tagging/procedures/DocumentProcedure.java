package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.controllers.DocumentController;
import com.castsoftware.tagging.controllers.TagController;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.results.NodeResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class DocumentProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "tagging.document.add", mode = Mode.WRITE)
    @Description("tagging.document.add(String Tag, String AssociatedRequest, Boolean Activation, String Description, Long ParentId) - Add a tag node and link it to a use case node.")
    public Stream<NodeResult> addTagNode(@Name(value = "Title") String title,
                                         @Name(value= "Request")  String request,
                                         @Name(value= "Activation") Boolean activation,
                                         @Name(value= "Description")  String description,
                                         @Name(value= "DocumentDescription")  String documentDescription,
                                         @Name(value= "ParentId")  Long parentId) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            String message = String.format("Adding a %s node with parameters { 'Title' : '%s', 'Activation' : %s, 'Request' : '%s', 'Description' : '%s', 'DocumentDescription' : '%s' }.",
                    TagNode.getLabel(), title, activation, request, description, documentDescription);
            nal.logInfo(message);

            Node n =  DocumentController.addDocumentNode(nal, title, request, activation, description, documentDescription, parentId);
            return Stream.of(new NodeResult(n));
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

}
