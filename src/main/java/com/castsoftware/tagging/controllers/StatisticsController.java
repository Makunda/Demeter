package com.castsoftware.tagging.controllers;

import com.castsoftware.tagging.config.Configuration;
import com.castsoftware.tagging.database.Neo4jAL;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.tagging.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.tagging.models.ConfigurationNode;
import com.castsoftware.tagging.models.StatisticNode;
import com.castsoftware.tagging.models.TagNode;
import com.castsoftware.tagging.models.UseCaseNode;
import com.castsoftware.tagging.statistics.FileLogger;
import com.castsoftware.tagging.statistics.Highlight;
import com.castsoftware.tagging.statistics.HighlightCategory;
import com.castsoftware.tagging.statistics.PreStatisticsLogger;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsController {

    private static final String ERROR_PREFIX = "STATCx";
    private static final String CONF_TO_STAT_REL = Configuration.get("neo4j.relationships.configuration.to_stats");

    public static String writePreExecutionStatistics(Neo4jAL nal, String applicationContext) throws Neo4jBadRequestException, Exception {

        List<TagNode> tagNodeList = TagNode.getAllNodes(nal);
        List<Highlight> highlightList = new ArrayList<>();

        int nExecution = 0;
        for(TagNode tn : tagNodeList) {
            try {
                // Ignored non active requests
                if(!tn.getActive()) continue;

                Long numAffected = tn.forecastRequest(applicationContext);

                String useCaseName = tn.getParentUseCase().getName();

                if(numAffected > 0) {
                    Highlight h = new Highlight(tn.getTag(), useCaseName, tn.getDescription(), numAffected.intValue());
                    highlightList.add(h);
                }

                nExecution++;
            } catch (Neo4jNoResult | Neo4jBadNodeFormatException neo4jNoResult) {
                nal.logError(String.format("Tag with Id '%d' produced an error during forecasting.", tn.getNodeId()), neo4jNoResult);
            }
        }

        List<StatisticNode> statList = StatisticNode.getAllNodes(nal);

        try (PreStatisticsLogger pl = new PreStatisticsLogger(applicationContext)){

            pl.flushBuffer();
            pl.writeStatistics(statList);
            pl.writeHighlights(highlightList);
            pl.save();
        } catch (Exception e) {
            throw e;
        }

        return String.format("%d tags and %d statistics were processed.",nExecution, statList.size());
    }

    public static Node addStatisticNode(Neo4jAL nal, String name, String request, Boolean active, String description, Long configurationId) throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        Node parent = nal.getNodeById(configurationId);

        Label configLabel = Label.label(ConfigurationNode.getLabel());

        // Check if the parent is either a Configuration Node or another use case
        if(!parent.hasLabel(configLabel)) {
            throw new Neo4jBadRequestException(String.format("Can only attach a %s node to a %s.", StatisticNode.getLabel(), ConfigurationNode.getLabel()),
                    ERROR_PREFIX + "ADDU1");
        }

        StatisticNode statNode = new StatisticNode(nal, name, request,  active, description);
        Node n = statNode.createNode();

        // Create the relation to the use case
        parent.createRelationshipTo(n, RelationshipType.withName(CONF_TO_STAT_REL));

        return n;
    }
}
