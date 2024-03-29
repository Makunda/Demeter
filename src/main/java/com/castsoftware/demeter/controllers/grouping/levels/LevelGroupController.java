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

package com.castsoftware.demeter.controllers.grouping.levels;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.imaging.Level5Node;
import com.castsoftware.demeter.services.levels.LevelService;
import com.castsoftware.demeter.services.levels.ObjectService;
import com.castsoftware.demeter.utils.LevelsUtils;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

// TODO : Rewrite this class to be compliant with AGrouping
public class LevelGroupController {

    // Imaging Conf
    private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String IMAGING_OBJECT_TAGS =
            Configuration.get("imaging.link.object_property.tags");
    private static final String IMAGING_AGGREGATES =
            Configuration.get("imaging.node.level_nodes.links");


    // Class Conf
    private static final String ERROR_PREFIX = "GROCx";

    private final Neo4jAL neo4jAL;
    private List<String> messageOutput;

    /**
     * Constructor
     *
     * @param neo4jAL Neo4j Access Lauer
     */
    public LevelGroupController(Neo4jAL neo4jAL) {
        this.neo4jAL = neo4jAL;
        this.messageOutput = new ArrayList<>();
    }

    /**
     * Group the levels in the list of applications
     * @param applications List of application to process
     * @return The list of nodes created
     */
    public List<Node> groupInApplications(List<String> applications) throws Neo4jQueryException {
        List<Node> createdNodes = new ArrayList<>();

        // Parse the list of the application
        for(String app : applications) {
            try {
                createdNodes.addAll(this.groupAllLevels(app));
            } catch (Exception | Neo4jQueryException e) {
                neo4jAL.logError(String.format("Failed to group levels in the following application [%s].",app), e);
                throw e;
            }
        }

        return createdNodes;
    }

    /**
     * Group demeter levels in every applications Entry point of the Grouping action
     *
     * @return The list of level created
     * @throws Neo4jQueryException If the Neo4j query or its parameter are incorrect
     */
    public List<Node> groupInAllApplications() throws Neo4jQueryException {
        try {
            String applicationReq =
                    "MATCH (o:Object) WHERE EXISTS (o.Tags) AND any(x in o.Tags WHERE x CONTAINS $tagPrefix) "
                            + "RETURN DISTINCT [ x in LABELS(o) WHERE NOT x='Object'][0] as application;";
            Map<String, Object> params = Map.of("tagPrefix", getLevelPrefix());

            Result res = neo4jAL.executeQuery(applicationReq, params);
            List<Node> fullResults = new ArrayList<>();
            List<String> applicationProcessed = new ArrayList<>();

            // Parse all the application
            while (res.hasNext()) {
                String application = (String) res.next().get("application");
                applicationProcessed.add(application);
                fullResults.addAll(this.groupAllLevels(application));
            }

            // Print the status of the execution
            String applicationsAsString = String.join(", ", applicationProcessed);
            if (fullResults.isEmpty()) {
                addStatus(
                        String.format(
                                "No Object tagged with prefix '%s' was found in applications :  [%s].",
                                getLevelPrefix(), applicationsAsString));
            } else {
                addStatus(
                        String.format(
                                "%d applications were processed. List: [%s]",
                                applicationProcessed.size(), applicationsAsString));
            }

            return fullResults;
        } catch (Exception | Neo4jQueryException err) {
            neo4jAL.logError("Failed to group levels in every application", err);
            addStatus("Process stopped due to an error.");
            throw err;
        } finally {
            // Print the status of the execution
            printStatus();
        }
    }

    /**
     * Get the Demeter Tag identifier
     *
     * @return
     */
    public String getLevelPrefix() {
        return Configuration.getBestOfALl("demeter.prefix.level_group");
    }

    /**
     * Group all the level present in an application Entry point of the Grouping action
     *
     * @param applicationContext Name of the Application concerned by the merge
     * @return The list of the created levels
     * @throws Neo4jQueryException
     */
    public List<Node> groupAllLevels(String applicationContext) throws Neo4jQueryException {
        Map<String, List<Node>> groupMap = new HashMap<>();

        try {
            // Get the list of nodes prefixed by dm_tag
            String forgedTagRequest =
                    String.format(
                            "MATCH (o:%1$s:`%2$s`) WHERE any( x in o.%3$s WHERE x CONTAINS '%4$s')  "
                                    + "WITH o, [x in o.%3$s WHERE x CONTAINS '%4$s'][0] as g "
                                    + "RETURN o as node, g as group;",
                            IMAGING_OBJECT_LABEL, applicationContext, IMAGING_OBJECT_TAGS, getLevelPrefix());

            Result res = neo4jAL.executeQuery(forgedTagRequest);

            // Build the map for each group as <Tag, Node list>
            while (res.hasNext()) {
                Map<String, Object> resMap = res.next();
                String group = (String) resMap.get("group");
                Node node = (Node) resMap.get("node");

                // Add to  the specific group
                if (!groupMap.containsKey(group)) {
                    groupMap.put(group, new ArrayList<>());
                }
                groupMap.get(group).add(node);
            }

            addStatus(
                    String.format(
                            "Found %d distinct tags in the application '%s'",
                            groupMap.size(), applicationContext));

            List<Node> resNodes = new ArrayList<>();
            List<String> faultyTags = new ArrayList<>();


            // Build a level 5 and attach the node list
            for (Map.Entry<String, List<Node>> entry : groupMap.entrySet()) {
                String groupName = entry.getKey();
                List<Node> nodeList = entry.getValue();

                if (nodeList.isEmpty()) continue;

                try {
                    // Group a tag in the application, and link the list of objects to it
                    Node n = groupSingleTag(applicationContext, groupName, nodeList);
                    resNodes.add(n);
                } catch (Exception | Neo4jNoResult err) {
                    neo4jAL.logError(
                            "An error occurred trying to create Level 5 for nodes with tags : " + groupName, err);
                    faultyTags.add(groupName);
                }
            }

            if (!faultyTags.isEmpty()) {
                addStatus(
                        String.format(
                                "[%s] produced an error when trying to group them. Check the logs.",
                                String.join(", ", faultyTags)));
            }

            addStatus(
                    String.format(
                            "%d levels have been created in application '%s'.",
                            resNodes.size(), applicationContext));

            return resNodes;
        } catch (Exception | Neo4jQueryException err) {
            neo4jAL.logError(
                    String.format("Failed to group levels in application '%s'", applicationContext), err);
            addStatus("Process stopped due to an error.");
            throw err;
        } finally {
            // Print the status of the execution
            printStatus();
        }
    }

    /**
     * Add a message to the message output
     *
     * @param message Message to be displayed
     */
    private void addStatus(String message) {
        this.messageOutput.add(message);
    }

    /**
     * Print the list of message present in {messageOutput}. And flush the lst
     */
    private void printStatus() {
        int it = 0;
        StringBuilder sb = new StringBuilder();
        for (String message : messageOutput) {
            it++;
            sb.append(String.format("%d - %s\n", it, message));
        }
        neo4jAL.logInfo("Status of Demeter:\n" + sb.toString());
        messageOutput = new ArrayList<>();
    }

    /**
     * Group a specific tag on the application
     *
     * @param applicationContext Name of the application
     * @param groupName          Name of the group
     * @param nodeList           List of node concerned by the grouping
     * @return
     * @throws Neo4jNoResult
     * @throws Neo4jQueryException
     */
    public Node groupSingleTag(String applicationContext, String groupName, List<Node> nodeList)
            throws Neo4jNoResult, Neo4jQueryException {

        // Aggregates relationship between levels and objects
        RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);

        // Retrieve most encountered Level 5
        Node oldLevel5Node = getCommonLevel5(nodeList);

        // find level 4 node attached to old level 5
        Optional<Node> optLevel4 = findLevel4(oldLevel5Node);
        assert optLevel4.isPresent() : "The level 5 selected must be linked to a level 4";
        Node level4 = optLevel4.get();

        // Get level Property
        String oldFullName = (String) oldLevel5Node.getProperty(Level5Node.getFullNameProperty());
        String[] splitArr = oldFullName.split("##");
        String level4FullName = String.join("##", Arrays.copyOf(splitArr, splitArr.length - 1));

        // Forge the name of the level by removing the tag identifier
        String newLevelName = groupName.replace(getLevelPrefix(), "");

        // Merge new Level 5 and labeled them with application's name
        Node newLevel5 = this.getOrCreateLevel5(applicationContext, newLevelName, level4FullName);

        // Link new level to Level 4
        level4.createRelationshipTo(newLevel5, aggregatesRel);
        addStatus("New Level5 and ancient level 4 were linked together");

        // Delete old relationships, to not interfere with the new level
        for (Node n : nodeList) {

            // Find and delete the relationship to the previous level
            Optional<Node> level = LevelService.getObjectLevel(n);
            if(level.isPresent()) {
                // Get the name of the current level
                String levelName = LevelService.getLevelFullName(level.get());
                // Apply the level name as a property on the object for rollback
                ObjectService.applyOriginalLevel(level.get(), levelName);
            }

            // Detach all the others levels
            LevelService.detachLevels(n);

            // Relink to new level
            newLevel5.createRelationshipTo(n, aggregatesRel);

            // Change the level name to the new one of each node
            ObjectService.applyLevel(n, newLevel5);
        }

        addStatus(
                String.format(
                        "%d object were detached from their previous level an re-attached to the group.",
                        nodeList.size()));

        neo4jAL.logInfo("Refreshing the new level relationships and recount elements.");

        // refresh new level
        LevelsUtils.refreshAllAbstractLevel(neo4jAL, applicationContext);

        addStatus("All the level in the application were refreshed.");

        // Clean the tag processed
        cleanTag(applicationContext, groupName);

        return newLevel5;
    }

    /**
     * Get the level 5 present in the node list. Level are returned as a map, with the key
     * corresponding to the level node and the value their frequency. The return map is sorted by
     * ascending order.
     *
     * @param nodeList List of the node used to extract level5
     * @return The map containing level5 nodes and their usage frequency as a Stream
     * @throws Neo4jNoResult If no Level 5 were detected
     */
    private Node getCommonLevel5(List<Node> nodeList) throws Neo4jNoResult, Neo4jQueryException {

        // Search level 5 using Id list
        List<Long> idList = nodeList.stream().map(Node::getId).collect(Collectors.toList());
        String req =
                "MATCH (o:Object)<-[:Aggregates]-(l:Level5) WHERE ID(o) IN $idList "
                        + "RETURN DISTINCT  COUNT(DISTINCT o), l as node ORDER BY COUNT(DISTINCT o) DESC";
        Map<String, Object> params = Map.of("idList", idList);
        Result res = neo4jAL.executeQuery(req, params);

        if (!res.hasNext())
            throw new Neo4jNoResult(
                    "Failed to find a Level5 attached to the tagged objects.", req, ERROR_PREFIX + "GETL5");

        return (Node) res.next().get("node");
    }


    /**
     * Find the Level4 node attached to the Level 5 with the specified ID
     *
     * @param level5 Level 5 to process
     * @return
     */
    private Optional<Node> findLevel4(Node level5) throws Neo4jQueryException {
        // Get associated Level 4 full name and create the level 5 nodes
        String reqLevel4 =
                "MATCH (l:Level5) WHERE ID(l)=$idLevel "
                        + "WITH l "
                        + "MATCH (l)<-[:Aggregates]-(l4:Level4) "
                        + "RETURN l4 as node LIMIT 1";
        Map<String, Object> paramsLevel4 = Map.of("idLevel", level5.getId());
        Result resultLevel4 = neo4jAL.executeQuery(reqLevel4, paramsLevel4);

        if (!resultLevel4.hasNext()) {
            addStatus(
                    String.format("Failed to find a level 4 attached to Level 5 with id '%d'.", level5.getId()));
            addStatus(
                    String.format(
                            "Please run : 'MATCH (l5:Level5)<-[:Aggregates]-(l4:Level4) WHERE ID(l5)=%d  RETURN DISTINCT l4' in the Neo4j console.",
                            level5.getId()));
            return Optional.empty();
        }

        Node levelNode = (Node) resultLevel4.next().get("node");
        return Optional.of(levelNode);
    }

    /**
     * Get an existing level 5 or create a new Node
     *
     * @param applicationContext Name of the application
     * @param levelName          Name of the level
     * @param level4FullName     FullName of the level 4 attached (used to build the fullName )
     * @return The node found or created
     * @throws Neo4jQueryException If the Query or its parameters are incorrect
     * @throws Neo4jNoResult       If the query returned no result
     */
    private Node getOrCreateLevel5(String applicationContext, String levelName, String level4FullName)
            throws Neo4jQueryException, Neo4jNoResult {
        String forgedLabel = Level5Node.getLabel() + ":`" + applicationContext + "`";
        String forgedFindLevel =
                String.format(
                        "MATCH (o:%1$s) WHERE o.%2$s='%3$s' RETURN o as node;",
                        forgedLabel, Level5Node.getNameProperty(), levelName);

        Node node = null;
        Result result = neo4jAL.executeQuery(forgedFindLevel);
        if (result.hasNext()) {
            // Module with same name was found, and results will be merge into it
            node = (Node) result.next().get("node");
            addStatus(String.format("Found an existing level 5 with name '%s'.", applicationContext));
        } else {
            // Creating a new Level5
            Label applicationLabel = Label.label(applicationContext);

            // Forge properties of the node
            String rCol = getRandomColor();
            String fullName = level4FullName + "##" + levelName;

            // Add the Manual Creation property
            Level5Node newLevel =
                    new Level5Node(neo4jAL, levelName, false, true, fullName, rCol, 5L, 0L, rCol);

            node = newLevel.createNode();
            newLevel.setAutoGeneratedProperty();
            node.addLabel(applicationLabel); // Add the label of the application to the node
            addStatus(
                    String.format(
                            "A new Level 5 was created since no other level have the same level name '%s'",
                            levelName));
        }

        return node;
    }

    /**
     * Clean a specific group in the application
     *
     * @param applicationContext Name of the application
     * @throws Neo4jQueryException If the query produced an error
     */
    public void cleanTag(String applicationContext, String group) throws Neo4jQueryException {
        // Once the operation is done, remove Demeter tag prefix tags
        String removeTagsQuery =
                String.format(
                        "MATCH (o:`%1$s`) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS $tag ] RETURN COUNT(o) as removedTags;",
                        applicationContext, IMAGING_OBJECT_TAGS);
        Map<String, Object> params = Map.of("tag", group);
        Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery, params);

        if (tagRemoveRes.hasNext()) {
            Long nDel = (Long) tagRemoveRes.next().get("removedTags");
            neo4jAL.logInfo("# " + nDel + " demeter tag (" + group + ") were removed from the database.");
        }
    }

    /**
     * Clean the application from the Demeter tags
     *
     * @param applicationContext Name of the application
     * @throws Neo4jQueryException
     */
    public void cleanAllTags(String applicationContext) throws Neo4jQueryException {
        // Once the operation is done, remove Demeter tag prefix tags
        String removeTagsQuery =
                String.format(
                        "MATCH (o:`%1$s`) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS '%3$s' ] RETURN COUNT(o) as removedTags;",
                        applicationContext, IMAGING_OBJECT_TAGS, getLevelPrefix());
        Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery);

        if (tagRemoveRes.hasNext()) {
            Long nDel = (Long) tagRemoveRes.next().get("removedTags");
            neo4jAL.logInfo("# " + nDel + " demeter 'group tags' were removed from the database.");
        }
    }

    /**
     * Generate a random color as a string of type rgb(n, n, n)
     *
     * @return Random color
     */
    private String getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(254 + 1);
        int g = random.nextInt(254 + 1);
        int b = random.nextInt(254 + 1);

        return String.format("rgb(%d, %d, %d)", r, g, b);
    }
}
