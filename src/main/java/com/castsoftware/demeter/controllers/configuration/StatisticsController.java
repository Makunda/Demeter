/*
 * Copyright (C) 2020  Hugo JOBY
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License v3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public v3
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.demeter.controllers.configuration;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;

import com.castsoftware.demeter.models.demeter.DocumentNode;
import com.castsoftware.demeter.models.demeter.StatisticNode;
import com.castsoftware.demeter.models.demeter.TagNode;
import com.castsoftware.demeter.models.demeter.UseCaseNode;
import com.castsoftware.demeter.results.demeter.StatisticResult;
import com.castsoftware.demeter.statistics.Highlights.Highlight;
import com.castsoftware.demeter.statistics.Highlights.HighlightType;
import com.castsoftware.demeter.statistics.PreStatisticsLogger;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsController {

    private static final String ERROR_PREFIX = "STATCx";
    private static final String USECASE_TO_STAT_REL = Configuration.get("neo4j.relationships.use_case.to_stats");
    private static final String USE_CASE_RELATIONSHIP = Configuration.get("neo4j.relationships.use_case.to_use_case");

    /**
     * Get the active statistics nodes
     * @param nal Neo4j Access Layer
     * @param configurationName Name of the configuration
     * @return The list of active statistics for the specified configuration
     * @throws Neo4jNoResult
     * @throws Neo4jBadRequestException
     * @throws Neo4jQueryException
     */
    public static List<StatisticNode> getSelectedStatistics(Neo4jAL nal, String configurationName)
            throws Neo4jNoResult, Neo4jBadRequestException, Neo4jQueryException {

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
     * Get the result of statistics
     * @param nal Neo4j Access Layer
     * @param applicationContext Application label to use
     * @param configurationName
     * @return List of statistics results
     * @throws Neo4jBadRequestException
     * @throws Neo4jNoResult
     * @throws Neo4jQueryException
     */
    public static List<StatisticResult> getStatisticsResult(Neo4jAL nal, String configurationName, String applicationContext)
            throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
        List<StatisticResult> resultList = new ArrayList<>();
        List<StatisticNode> nodes = getSelectedStatistics(nal, configurationName);

        for(StatisticNode sn : nodes) {
            String result = sn.executeStat(applicationContext);
            StatisticResult sr = new StatisticResult(sn.getName(), sn.getDescription(), result);
            resultList.add(sr);
        }

        return resultList;
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
    public static List<String> writePreExecutionStatistics(Neo4jAL nal, String configurationName, String applicationContext) throws Neo4jBadRequestException, Exception, Neo4jQueryException, Neo4jNoResult {

        List<String> returnList = new ArrayList<>();
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

        returnList.add(String.format("%d tags and %d statistics were processed.",nExecution, statList.size()));
        returnList.add(String.format("The report was saved at '%s'.", PreStatisticsLogger.getOutputDirectory()));

        return returnList;
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
