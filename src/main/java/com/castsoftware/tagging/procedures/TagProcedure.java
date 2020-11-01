package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.controllers.TagController;
import com.castsoftware.tagging.controllers.UtilsController;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.TagNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class TagProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "tagging.tag.add", mode = Mode.WRITE)
    @Description("tagging.tag.add(String Tag, Boolean Activation, String AssociatedRequest, Long ParentId) - Add a tag node and link it to a use case node.")
    public Stream<Node> addTagnode(@Name(value = "Tag") String tag,
                                   @Name(value= "Activation") Boolean activation,
                                   @Name(value= "AssociatedRequest")  String associatedRequest,
                                   @Name(value= "ParentId")  Long parentId) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);

            String message = String.format("Adding a %s node with parameters { 'Tag' : '%s', 'Activation' : %b, 'Request' : '%s' }.",
                    TagNode.getLabel(), tag, activation, associatedRequest);
            nal.info(message);

            Node n =  TagController.addTagNode(nal, tag, activation, associatedRequest, parentId);

            return Stream.of(n);
        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequest | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

}
