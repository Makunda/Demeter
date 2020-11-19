package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.*;

import com.castsoftware.tagging.statistics.Highlights.Highlight;
import com.castsoftware.tagging.statistics.Highlights.HighlightType;
import com.castsoftware.tagging.statistics.PreStatisticsLogger;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsController {

    private static final String ERROR_PREFIX = "STATCx";
    private static final String USECASE_TO_STAT_REL = Configuration.get("neo4j.relationships.use_case.to_stats");
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");


    public static List<StatisticNode> getSelectedStatistics(Neo4jAL nal, String configurationName) throws Neo4jNoResult, Neo4jBadRequestException, Neo4jQueryException {

        Label statisticsLabel = Label.label(StatisticNode.getLabel());
        Set<Node> statistics = UseCaseController.searchByLabelInActiveBranches(nal, configurationName, statisticsLabel);

        //TagNode.fromNode(neo4jAL, otherNode)
        return statistics.stream().map( x -> {
            try {
                return StatisticNode.fromNode(nal, x);
            } catch (Neo4jBadNodeFormatException ex) {
                nal.getLogger().error("Error during Tag Nodes discovery.", ex);
                return null;
            }
        }).filter(x -> x != null && x.getActive()).collect(Collectors.toList());
    }

    /**
     * Write the pre-execution statistics to a File.
     * The pre-execution statistics act like recommendations of potential interesting useCases that you want to address.
     * @param nal Neo4j Access Layer
     * @param configurationName Name of the configuration
     * @param applicationContext Context of the application
     * @return Number of statistics processed ( Formatted String )
     * @throws Neo4jBadRequestException
     * @throws Exception
     * @throws Neo4jQueryException
     * @throws Neo4jNoResult
     */
    public static String writePreExecutionStatistics(Neo4jAL nal, String configurationName, String applicationContext) throws Neo4jBadRequestException, Exception, Neo4jQueryException, Neo4jNoResult {

        List<Highlight> highlightList = new ArrayList<>();
        List<TagNode> tagNodeList = TagController.getSelectedTags(nal, configurationName);

        int nExecution = 0;
        for(TagNode tn : tagNodeList) {
            try {
                // Ignored non active requests
                if(!tn.getActive()) continue;

                Long numAffected = tn.forecastRequest(applicationContext);
                String useCaseName = tn.getParentUseCase().getName();

                if(numAffected > 0) {
                    Highlight h = new Highlight(tn.getTag(), useCaseName, tn.getDescription(), numAffected.intValue(), HighlightType.TAG);
                    highlightList.add(h);
                }

                nExecution++;
            } catch (Neo4jNoResult | Neo4jBadNodeFormatException neo4jNoResult) {
                nal.logError(String.format("Tag with Id '%d' produced an error during forecasting.", tn.getNodeId()), neo4jNoResult);
            }
        }

        List<DocumentNode> documentNodeList = DocumentController.getSelectedDocuments(nal, configurationName);

        for (DocumentNode doc : documentNodeList) {
            try {
                // Ignored non active requests
                if(!doc.getActive()) continue;

                Long numAffected = doc.forecastRequest(applicationContext);

                UseCaseNode parent = doc.getParentUseCase();
                String useCaseName = "Unknown";

                if(parent != null) {
                    useCaseName= parent.getName();
                }

                if(numAffected > 0) {
                    Highlight h = new Highlight(doc.getTitle(), useCaseName, doc.getDescription(), numAffected.intValue(), HighlightType.DOCUMENT);
                    highlightList.add(h);
                }

                nExecution++;
            } catch (Neo4jNoResult | Neo4jBadNodeFormatException neo4jNoResult) {
                nal.logError(String.format("Tag with Id '%d' produced an error during forecasting.", doc.getNodeId()), neo4jNoResult);
            }
        }

        List<StatisticNode> statList = getSelectedStatistics(nal, configurationName);

        try (PreStatisticsLogger pl = new PreStatisticsLogger(applicationContext)){

            pl.flushBuffer();
            pl.writeStatistics(statList);
            pl.writeHighlights(highlightList);
            pl.save();
        }

        return String.format("%d tags and %d statistics were processed.",nExecution, statList.size());
    }

    public static Node addStatisticNode(Neo4jAL nal, String name, String request, Boolean active, String description, Long useCaseId) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        Node parent = nal.getNodeById(useCaseId);

        Label useCaseLabel = Label.label(UseCaseNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(useCaseLabel)) {
            throw new Neo4jBadRequestException(String.format("Can only attach a %s node to a %s.", StatisticNode.getLabel(), UseCaseNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        StatisticNode statNode = new StatisticNode(nal, name, request,  active, description);
        Node n = statNode.createNode();

        // Create the relation to the use case
        parent.createRelationshipTo(n, RelationshipType.withName(USECASE_TO_STAT_REL));

        return n;
    }
}
