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

package com.castsoftware.demeter.controllers.api;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.config.UserConfiguration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.results.demeter.CandidateFindingResult;
import com.castsoftware.demeter.results.demeter.DemeterGroupResult;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupingController {
    // Community prefix

    /**
     * Get the tag prefix for the level 5
     * @return
     */
    public static String getLevelGroupPrefix() {
        return Configuration.getBestOfALl("demeter.prefix.level_group");
    }


    /**
     * Set the tag prefix for the level 5
     * @param prefix New prefix for the level 5 grouping
     * @return The new prefix
     * @throws MissingFileException
     */
    public static String setLevelGroupPrefix(String prefix) throws MissingFileException, FileNotFoundException {
        Configuration.setEverywhere("demeter.prefix.level_group", prefix);
        return prefix;
    }

    // Module prefix

    /**
     * Get the tag prefix for the modules
     * @return
     */
    public static String getModuleGroupPrefix() {
        return Configuration.getBestOfALl("demeter.prefix.module_group");
    }

    /**
     * Set the tag prefix for the module grouping
     * @param prefix New prefix for the module 5 grouping
     * @return
     * @throws MissingFileException
     */
    public static String setModuleGroupPrefix(String prefix) throws MissingFileException, FileNotFoundException {
        return Configuration.setEverywhere("demeter.prefix.module_group", prefix);
    }

    // View prefix

    /**
     * Get the tag prefix for the view grouping
     * @return
     */
    public static String getViewGroupPrefix() {
        return UserConfiguration.get("demeter.prefix.view_group");
    }

    /**
     * Set the tag  prefix for the view grouping
     * @param prefix New prefix for the grouping
     * @return
     */
    public static String setViewGroupPrefix(String prefix) throws MissingFileException, FileNotFoundException {
        return Configuration.setEverywhere("demeter.prefix.view_group", prefix);
    }

    // Architecture prefix

    /**
     * Get the tag prefix for the architecture grouping
     * @return
     */
    public static String getArchitectureGroupPrefix() {
        return Configuration.getBestOfALl("demeter.prefix.architecture_group");
    }

    /**
     * Set the tag prefix for the architecture grouping
     * @param prefix New prefix for the grouping
     * @return
     */
    public static String getArchitectureGroupPrefix(String prefix) throws MissingFileException, FileNotFoundException {
        return Configuration.setEverywhere("demeter.prefix.architecture_group", prefix);
    }


    /**
     * Get the number of application concerned by a prefixed tag in the database
     * @param neo4jAL Neo4j Access Layer
     * @param tagPrefix Name of the prefix to look for
     * @return A list of match as a list of GroupingResult
     * @throws Neo4jQueryException
     */
    private static List<CandidateFindingResult> getGroupingResults(Neo4jAL neo4jAL, String tagPrefix) throws Neo4jQueryException {
        List<CandidateFindingResult> candidateFindingResults = new ArrayList<>();

        String request = "MATCH (app:Application) " +
                "WITH [app.Name] as appName " +
                "MATCH (o:Object) WHERE EXISTS(o.Tags) " +
                "AND any( x IN o.Tags WHERE x CONTAINS $tagPrefix ) " +
                "AND any( x IN LABELS(o) WHERE x IN appName) " +
                "RETURN DISTINCT [ x IN LABELS(o) WHERE x IN appName][0] as application , [x IN o.Tags WHERE x CONTAINS $tagPrefix] as tags,  COUNT(o) as numTags";

        Map<String, Object> parameters = Map.of("tagPrefix", tagPrefix);
        Result res = neo4jAL.executeQuery(request, parameters);

        while(res.hasNext()) {
            Map<String, Object> result = res.next();
            String applicationName = (String) result.get("application");
            String[] tags = (String[]) result.get("tags");
            Long numTags = (Long) result.get("numTags");

            candidateFindingResults.add(new CandidateFindingResult(applicationName, tags, numTags));
        }

        return candidateFindingResults;
    }

    /**
     * Get the number of application concerned by a tag in the database
     * @param neo4jAL Neo4j Access Layer
     * @param tagPrefix Name of the prefix to look for
     * @return A list of match as a list of GroupingResult
     * @throws Neo4jQueryException
     */
    private static List<CandidateFindingResult> getGroupingResultsOneApplication(Neo4jAL neo4jAL, String tagPrefix, String application) throws Neo4jQueryException {
        List<CandidateFindingResult> candidateFindingResults = new ArrayList<>();

        String request = "MATCH (app:Application) WHERE app.Name=$appName " +
                "WITH [app.Name] as appName " +
                "MATCH (o:Object) WHERE EXISTS(o.Tags) " +
                "AND any( x IN o.Tags WHERE x CONTAINS $tagPrefix ) " +
                "AND any( x IN LABELS(o) WHERE x IN appName) " +
                "RETURN DISTINCT [ x IN LABELS(o) WHERE x IN appName][0] as application , [x IN o.Tags WHERE x CONTAINS $tagPrefix] as tags,  COUNT(o) as numTags";

        Map<String, Object> parameters = Map.of("tagPrefix", tagPrefix, "appName", application);
        Result res = neo4jAL.executeQuery(request, parameters);

        while(res.hasNext()) {
            Map<String, Object> result = res.next();
            String applicationName = (String) result.get("application");
            String[] tags = (String[]) result.get("tags");
            Long numTags = (Long) result.get("numTags");

            candidateFindingResults.add(new CandidateFindingResult(applicationName, tags, numTags));
        }

        return candidateFindingResults;
    }

    /**
     * Get a list of the group with the specified prefix
     * @param neo4jAL Neo4j Access Layer
     * @param application Name of the application
     * @param groupPrefix Prefix of the groups
     * @return
     * @throws Neo4jQueryException
     */
    private static List<DemeterGroupResult> getDemeterGroupedOneApplication(Neo4jAL neo4jAL, String application, String groupPrefix) throws Neo4jQueryException {
        List<DemeterGroupResult> demeterGroupResults = new ArrayList<>();

        String request = String.format("MATCH (l:Level5:`%s`)-[:Aggregates]->(o:Object) WHERE l.FullName=~'.*##%s(.*)' RETURN ID(l) as id, l.Name as groupName, COUNT(o) as numObjects ;", application, groupPrefix);
        Result res = neo4jAL.executeQuery(request);

        while(res.hasNext()) {
            Map<String, Object> result = res.next();
            Long id = (Long) result.get("id");
            String groupName = (String) result.get("groupName");
            Long numObjects = (Long) result.get("numObjects");

            demeterGroupResults.add(new DemeterGroupResult(id, groupName, application, numObjects));
        }

        return demeterGroupResults;

    }




    /**
     * Get the candidates application for the Level Grouping
     * @param neo4jAL Neo4j access Layer
     * @return
     * @throws Neo4jQueryException
     */
    public static List<CandidateFindingResult> getCandidateApplicationsLevelGroup(Neo4jAL neo4jAL) throws Neo4jQueryException {
        return getGroupingResults(neo4jAL, getLevelGroupPrefix());
    }

    public static List<CandidateFindingResult> getCandidateApplicationsLevelGroup(Neo4jAL neo4jAL, String application) throws Neo4jQueryException {
        return getGroupingResultsOneApplication(neo4jAL, getLevelGroupPrefix(), application);
    }

    /**
     * Get the candidates application for the Module Grouping
     * @param neo4jAL Neo4j access Layer
     * @return
     * @throws Neo4jQueryException
     */
    public static List<CandidateFindingResult> getCandidateApplicationsModuleGroup(Neo4jAL neo4jAL) throws Neo4jQueryException {
        return getGroupingResults(neo4jAL, getModuleGroupPrefix());
    }

    public static List<CandidateFindingResult> getCandidateApplicationsModuleGroup(Neo4jAL neo4jAL, String application) throws Neo4jQueryException {
        return getGroupingResultsOneApplication(neo4jAL, getModuleGroupPrefix(), application);
    }

    /**
     * Get the demeter levels in an application
     * @param neo4jAL Neo4j Access Layer
     * @param application
     * @return
     * @throws Neo4jQueryException
     */
    public static List<DemeterGroupResult> getDemeterLevels(Neo4jAL neo4jAL, String application) throws Neo4jQueryException {
        String generatedLevelPrefix = Configuration.get("demeter.prefix.generated_level_prefix");
        return getDemeterGroupedOneApplication(neo4jAL, application, generatedLevelPrefix);
    }

    /**
     * Get the demeter modules in an application
     * @param neo4jAL Neo4j Access Layer
     * @param application
     * @return
     * @throws Neo4jQueryException
     */
    public static List<DemeterGroupResult> getDemeterModules(Neo4jAL neo4jAL, String application) throws Neo4jQueryException {
        String generatedLevelPrefix = Configuration.get("demeter.prefix.generated_module_prefix");
        return getDemeterGroupedOneApplication(neo4jAL, application, generatedLevelPrefix);
    }


}
