package com.castsoftware.tagging.procedures;

import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.ProcedureException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequest;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.controllers.UseCaseController;
import com.castsoftware.tagging.models.UseCaseNode;
import com.castsoftware.tagging.results.NodeResult;
import com.castsoftware.tagging.results.UseCasesMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class UseCaseProcedure {
    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    @Procedure(value = "tagging.useCases.add", mode = Mode.WRITE)
    @Description("tagging.useCases.add( Long idParent, String name, Boolean active) - Add a use case to a configuration node or another usecase node.")
    public Stream<NodeResult> addUseCase(@Name(value="idParent") Long idParent, @Name(value="Name") String name, @Name(value="Active", defaultValue = "False") Boolean active ) throws ProcedureException {

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction
            nal.info(String.format("Adding a use case with parameters { 'Name' : '%s' , 'Active' : %b } ", name, active));

            Node n = UseCaseController.addUseCase(nal, name, active, idParent);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();

            return Stream.of(new NodeResult(n));

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequest | Neo4jNoResult e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }

    }

    @Procedure(value = "tagging.useCases.list", mode = Mode.WRITE)
    @Description("tagging.useCases.list() - List all the use cases present.")
    public Stream<UseCasesMessage> listUseCaseNodes() throws ProcedureException {

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction
            nal.info("Starting Use Case Listing..");

            List<UseCaseNode> useCases = UseCaseController.listUseCases(nal);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();

            return useCases.stream().map(UseCasesMessage::new);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequest e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

    @Procedure(value = "tagging.useCases.activate", mode = Mode.WRITE)
    @Description("tagging.useCases.activate(Long idUseCase) - Set the activation of the use case node and all other nodes under it.")
    public Stream<UseCasesMessage> activateUseCase() throws ProcedureException {

        try (Neo4jAL nal = new Neo4jAL(db, log)){
            nal.openTransaction(); // Open the transaction
            nal.info("Starting Use Case Listing..");

            List<UseCaseNode> useCases = UseCaseController.listUseCases(nal);

            nal.commitTransaction(); // Commit the transaction
            nal.closeTransaction();

            return useCases.stream().map(UseCasesMessage::new);

        } catch (Exception | Neo4jConnectionError | Neo4jQueryException | Neo4jBadRequest e) {
            ProcedureException ex = new ProcedureException(e);
            ex.logException(log);
            throw ex;
        }
    }

}
