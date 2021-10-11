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
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.imaging.Level5Node;
import com.castsoftware.demeter.utils.LevelsUtils;
import org.neo4j.graphdb.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

// TODO : Rewrite this class to be compliant with AGrouping
public class LevelGroupController {

  // Imaging Conf
  private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
  private static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");
  private static final String IMAGING_OBJECT_LEVEL = Configuration.get("imaging.node.object.level");
  private static final String IMAGING_AGGREGATES =
      Configuration.get("imaging.node.level_nodes.links");
  private static final String IMAGING_LEVEL4_LABEL = Configuration.get("imaging.node.level4.label");
  private static final String IMAGING_LEVEL5_LABEL = Configuration.get("imaging.node.level5.label");

  // Demeter Conf
  private static final String GENERATED_LEVEL_IDENTIFIER =
          Configuration.get("demeter.prefix.generated_level_prefix");

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
   * Group demeter levels in every applications
   * Entry point of the Grouping action
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
        addStatus(String.format("No Object tagged with prefix '%s' was found in applications :  [%s].",
                getLevelPrefix(), applicationsAsString));
      } else {
        addStatus(String.format("%d applications were processed. List: [%s]", applicationProcessed.size(), applicationsAsString));
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
   * Group all the level present in an application
   * Entry point of the Grouping action
   *
   * @param applicationContext Name of the Application concerned by the merge
   * @return
   * @throws Neo4jQueryException
   */
  public List<Node> groupAllLevels(String applicationContext)
          throws Neo4jQueryException {
    Map<String, List<Node>> groupMap = new HashMap<>();

    try {
      addStatus(String.format("Starting Demeter level 5 grouping in application '%s'", applicationContext));

      // Hot Fix Sanitize Application name

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

      addStatus(String.format("Found %d distinct tags in the application '%s'", groupMap.size(), applicationContext));

      List<Node> resNodes = new ArrayList<>();
      List<String> faultyTags = new ArrayList<>();
      // Build a level 5 and attach the node list
      for (Map.Entry<String, List<Node>> entry : groupMap.entrySet()) {
        String groupName = entry.getKey();
        List<Node> nodeList = entry.getValue();

        if (nodeList.isEmpty()) continue;

        try {
          Node n = groupSingleTag(applicationContext, groupName, nodeList);
          resNodes.add(n);
        } catch (Exception
                | Neo4jNoResult err) {
          neo4jAL.logError(
                  "An error occurred trying to create Level 5 for nodes with tags : " + groupName, err);
          faultyTags.add(groupName);
        }
      }

      if (!faultyTags.isEmpty()) {
        addStatus(String.format("[%s] produced an error when trying to group them. Check the logs.", String.join(", ", faultyTags)));
      }

      addStatus(String.format("%d levels have been created in application '%s'.", resNodes.size(), applicationContext));

      return resNodes;
    } catch (Exception | Neo4jQueryException err) {
      neo4jAL.logError(String.format("Failed to group levels in application '%s'", applicationContext), err);
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
   * Print the list of message present in {messageOutput}.
   * And flush the lst
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
   * Create backup node for each node in the list
   * @param application Name of the application
   * @param nodeList List of node to backup
   */
  private void createBackups(String application, List<Node> nodeList) {
    // For each node get the Level 5
    Map<Level5Node, List<Node>> toBackup = new HashMap<>();

    int failed = 0;
    int critical = 0;

    String req = "MATCH (o:Object)<-[:Aggregates]-(l:Level5) WHERE ID(o)=$idNode AND NOT l.FullName CONTAINS $genPrefix " +
            "RETURN DISTINCT l as level";
    Map<String, Object> params;
    Result res;
    for (Node n : nodeList) {
      try {
        params = Map.of("idNode", n.getId(), "genPrefix", GENERATED_LEVEL_IDENTIFIER);
        res = neo4jAL.executeQuery(req, params);

        if(!res.hasNext()) continue;

        Node levelNode = (Node) res.next().get("level");
        Level5Node l5 = Level5Node.fromNode(neo4jAL, levelNode);

        if(!toBackup.containsKey(l5)) toBackup.put(l5, new ArrayList<>());
        toBackup.get(l5).add(n);

      } catch (Exception | Neo4jQueryException | Neo4jBadNodeFormatException error) {
        failed++;
        neo4jAL.logError("Failed to backup ", error);
      }
    }

    // Create Backup nodes
    for (Map.Entry<Level5Node, List<Node>> en : toBackup.entrySet()) {
      try {
        Level5Node  ln = en.getKey();
        List<Node> toBackupList = en.getValue();

        ln.createLevel5Backup(application, toBackupList);
      } catch (Exception | Neo4jBadRequestException | Neo4jNoResult | Neo4jQueryException err) {
        critical++;
        neo4jAL.logError(String.format("Failed to create a backup for level with name '%s'.", en.getKey().getName()), err);
      }
    }

    addStatus(String.format("%d Objects are backup by %d backup nodes.", nodeList.size(), toBackup.size()));

    if(failed != 0) {
      addStatus(String.format("Failed to backup %d nodes due to bad node format. Not critical/important.", failed));
    }

    if(critical != 0) {
      addStatus(String.format("Failed to create %d backup nodes. Critical error.", failed));
    }
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
   * @throws Neo4jBadNodeFormatException
   * @throws Neo4jBadRequestException
   */
  public Node groupSingleTag(
          String applicationContext, String groupName, List<Node> nodeList)
          throws Neo4jNoResult, Neo4jQueryException {

    RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);

    // Timer used to follows the time taken by each steps
    Instant startTimer;
    Instant finishTimer;

    startTimer = Instant.now();

    // Retrieve most encountered Level 5
    Node oldLevel5Node = getLevel5(nodeList);


    // Create backup for the levels
    createBackups(applicationContext, nodeList);


    if (oldLevel5Node == null) {
      addStatus(String.format("Failed to find a Level 5 attached to Objects with tags '%s'.", groupName));
      addStatus(String.format("Please run : 'MATCH (o:Object:`%s`)<-[:Aggregates]-(l:Level5) WHERE '%s' IN o.Tags  RETURN DISTINCT l' in the Neo4j console.", applicationContext, groupName));
      throw new Neo4jNoResult("No Level 5 is attached to the selected nodes.", "Null parent Level5", ERROR_PREFIX + "GROT1");
    }

    finishTimer = Instant.now();
    neo4jAL.logInfo(
            String.format(
                    "Level 5 found, with Id : %d has been identified in %d Milliseconds.",
                    oldLevel5Node.getId(), Duration.between(startTimer, finishTimer).toMillis()));
    addStatus(String.format("Level 5 with name '%s' is used as a backup node in application '%s'.", (String) oldLevel5Node.getProperty("Name"), applicationContext));


    neo4jAL.logInfo("Creating new level 5 node ... ");

    // find level 4 node attached to old level 5
    Node level4 = findLevel4(oldLevel5Node.getId());

    // Get level Property
    String oldFullName = (String) oldLevel5Node.getProperty(Level5Node.getFullNameProperty());
    String[] splitArr = oldFullName.split("##");
    String level4FullName = String.join("##", Arrays.copyOf(splitArr, splitArr.length - 1));

    // Forge the name of the level by removing the tag identifier
    String newLevelName = groupName.replace(getLevelPrefix(), "");

    // Merge new Level 5 and labeled them with application's name
    Node newLevel5 = getOrCreateLevel5(applicationContext, newLevelName, level4FullName);

    // Link new level to Level 4
    level4.createRelationshipTo(newLevel5, aggregatesRel);
    ;
    addStatus("New Level5 and ancient level 4 were linked together");

    startTimer = Instant.now();

    // Delete old relationships, to not interfere with the new level
    for (Node n : nodeList) {
      // Find and Delete Old Relationships
      for (Relationship relN : n.getRelationships(Direction.INCOMING, aggregatesRel)) {
        relN.delete();
      }

      // Relink to new level
      newLevel5.createRelationshipTo(n, aggregatesRel);
      // Change the level name to the new one of each node
      n.setProperty(IMAGING_OBJECT_LEVEL, newLevelName);
    }

    addStatus(String.format("%d object were detached from their previous level an re-attached to the group.", nodeList.size()));

    neo4jAL.logInfo("Refreshing the new level relationships and recount elements.");

    //LevelsUtils.refreshLevel5(neo4jAL, applicationContext, newLevel5); // refresh new level
    LevelsUtils.refreshAllAbstractLevel(neo4jAL, applicationContext);

    addStatus("All the level in the application were refreshed.");

    // Clean the tag processes
    cleanTag(applicationContext, groupName);

    return newLevel5;
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
   * Clean a specific group in the application
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
      neo4jAL.logInfo("# " + nDel + " demeter tag ("+group+") were removed from the database.");
    }
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
  private Node getLevel5(List<Node> nodeList)
          throws Neo4jNoResult, Neo4jQueryException {

    // Search level 5 using Id list
    List<Long> idList = nodeList.stream().map(Node::getId).collect(Collectors.toList());
    String req = "MATCH (o:Object)<-[:Aggregates]-(l:Level5) WHERE ID(o) IN $idList " +
            "RETURN DISTINCT  COUNT(DISTINCT o), l as node ORDER BY COUNT(DISTINCT o) DESC";
    Map<String, Object> params = Map.of("idList", idList);
    Result res = neo4jAL.executeQuery(req, params);

    if (!res.hasNext())
      throw new Neo4jNoResult("Failed to find a Level5 attached to the tagged objects.", req, ERROR_PREFIX + "GETL5");

    return (Node) res.next().get("node");
  }

  /**
   * Find the Level4 node attached to the Level 5 with the specified ID
   *
   * @param idLevel5 Id of the level 5
   * @return
   */
  private Node findLevel4(Long idLevel5) throws Neo4jQueryException {
    // Get associated Level 4 full name and create the level 5 nodes
    String reqLevel4 = "MATCH (l:Level5) WHERE ID(l)=$idLevel " +
            "WITH l " +
            "MATCH (l)<-[:Aggregates]-(l4:Level4) " +
            "RETURN l4 as node LIMIT 1";
    Map<String, Object> paramsLevel4 = Map.of("idLevel", idLevel5);
    Result resultLevel4 = neo4jAL.executeQuery(reqLevel4, paramsLevel4);

    if (!resultLevel4.hasNext()) {
      addStatus(String.format("Failed to find a level 4 attached to Level 5 with id '%d'.",
              idLevel5));
      addStatus(String.format("Please run : 'MATCH (l5:Level5)<-[:Aggregates]-(l4:Level4) WHERE ID(l5)=%d  RETURN DISTINCT l4' in the Neo4j console.", idLevel5));
    }

    return (Node) resultLevel4.next().get("node");
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
  private Node getOrCreateLevel5(String applicationContext, String levelName, String level4FullName) throws Neo4jQueryException, Neo4jNoResult {
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
      // Create a new Level5
      Label applicationLabel = Label.label(applicationContext);
      // Forge properties
      String rCol = getRandomColor();
      String fullName = level4FullName + "##" + GENERATED_LEVEL_IDENTIFIER + levelName;

      Level5Node newLevel =
              new Level5Node(neo4jAL, levelName, false, true, fullName, rCol, 5L, 0L, rCol);
      node = newLevel.createNode();
      node.addLabel(applicationLabel); // Add the label of the application to the node

      addStatus(String.format("A new Level 5 was created since no other level have the same level name '%s'", levelName));
    }

    return node;
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
