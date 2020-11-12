package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.controllers.UseCaseController;
import com.castsoftware.tagging.models.UseCaseNode;
import com.castsoftware.tagging.results.NodeResult;
import com.castsoftware.tagging.results.OutputMessage;
import com.castsoftware.tagging.results.UseCasesMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class UseCaseProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Transaction transaction;

    @Context
    public Log log;

    @Procedure(value = "tagging.useCases.add", mode = Mode.WRITE)
    @Description("tagging.useCases.add( Long idParent, String name, Boolean active) - Add a use case to a configuration node or another usecase node.")
    public Stream<NodeResult> addUseCase(@Name(value="idParent") Long idParent, @Name(value="Name") String name, @Name(value="Active", defaultValue = "False") Boolean active ) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo(String.format("Adding a use case with parameters { 'Name' : '%s' , 'Active' : %b } ", name, active));

            Node n = UseCaseController.addUseCase(nal, name, active, idParent);

            nal.logInfo("Done !");

            return Stream.of(new NodeResult(n));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }

    @Procedure(value = "tagging.useCases.list", mode = Mode.WRITE)
    @Description("tagging.useCases.list() - List all the use cases present.")
    public Stream<UseCasesMessage> listUseCaseNodes() throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            nal.logInfo("Starting Use Case Listing..");

            List<UseCaseNode> useCases = UseCaseController.listUseCases(nal);

            return useCases.stream().map(UseCasesMessage::new);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    /**
     * Activate a use case node and all its children.
     * @param idUseCase Id of the use case
     * @param activation Value that will be set to all matching nodes.
     * @return The list of all node concerned by the modification
     * @throws ProcedureException
     */
    @Procedure(value = "tagging.useCases.activate", mode = Mode.WRITE)
    @Description("tagging.useCases.activate(Long idUseCase, Boolean Activation) - Set the activation of the use case node and all other nodes under it.")
    public Stream<UseCasesMessage> activateUseCase(@Name(value="Id") Long idUseCase, @Name(value="Activation") Boolean activation) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            List<UseCaseNode> useCases = UseCaseController.selectUseCase(nal, idUseCase, activation);
            
            return useCases.stream().map(UseCasesMessage::new);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequestException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    @Procedure(value = "tagging.useCases.globalActivation", mode = Mode.WRITE)
    @Description("tagging.useCases.globalActivation(Boolean Activation) - Set the activation of every use case nodes.")
    public Stream<OutputMessage> globalActivationUseCase(@Name(value="Activation") Boolean activation) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            int nModifications= UseCaseController.activateAllUseCase(nal, activation);

            String message = String.format("The activation parameter is now set to \"%b\" on %d nodes.", activation, nModifications);

            return Stream.of(new OutputMessage(message));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    @Procedure(value = "tagging.useCases.globalSelection", mode = Mode.WRITE)
    @Description("tagging.useCases.globalSelection(Boolean Activation) - Set the Selection of every use case nodes.")
    public Stream<OutputMessage> globalSelectionUseCase(@Name(value="Activation") Boolean activation) throws ProcedureException {

        try {
            Neo4jAL nal = new Neo4jAL(db, transaction, log);
            int nModifications= UseCaseController.selectAllUseCase(nal, activation);

            String message = String.format("The selection parameter is now set to \"%b\" on %d nodes.", activation, nModifications);

            return Stream.of(new OutputMessage(message));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

}
