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

  // Demeter Conf
  private static final String GENERATED_LEVEL_IDENTIFIER =
      Configuration.get("demeter.prefix.generated_level_prefix");

  // Class Conf
  private static final String ERROR_PREFIX = "GROCx";

  /**
   * Get the Demeter Tag identifier
   *
   * @return
   */
  public static String getLevelPrefix() {
    return Configuration.getBestOfALl("demeter.prefix.level_group");
  }

  /**
   * Get the level 5 present in the node list. Level are returned as a map, with the key
   * corresponding to the level node and the value their frequency. The return map is sorted by
   * ascending order.
   *
   * @param neo4jAL Neo4j Access Layer
   * @param nodeList List of the node used to extract level5
   * @return The map containing level5 nodes and their usage frequency as a Stream
   * @throws Neo4jNoResult If no Level 5 were detected
   */
  private static Iterator<Map.Entry<Node, Integer>> getLevel5(Neo4jAL neo4jAL, List<Node> nodeList)
      throws Neo4jNoResult {

    RelationshipType relLevel = RelationshipType.withName(IMAGING_AGGREGATES);

    // Get Actual Level 5 and connections
    Map<Node, Integer> level5map = new HashMap<>();

    for (Node rObject : nodeList) {

      Iterator<Relationship> oldRel =
          rObject.getRelationships(Direction.INCOMING, relLevel).iterator();
      if (oldRel.hasNext()) {
        Node level5 = oldRel.next().getStartNode();

        level5map.putIfAbsent(level5, 0);
        level5map.compute(level5, (x, v) -> v + 1);
      }
    }

    if (level5map.size() == 0) {
      throw new Neo4jNoResult(
          "Cannot find a valid Level 5 for the tag",
          "No relation detected between tagged node and Level5",
          ERROR_PREFIX + "GROS1");
    }

    return level5map.entrySet().stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .iterator();
  }

  /**
   * Clean the application from the Demeter tags
   *
   * @param neo4jAL
   * @param applicationContextW
   * @throws Neo4jQueryException
   */
  public static void clean(Neo4jAL neo4jAL, String applicationContext) throws Neo4jQueryException {
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
   * Group a specific tag on the application
   *
   * @param neo4jAL Neo4j Access layer
   * @param applicationContext Name of the application
   * @param groupName Name of the group
   * @param nodeList List of node concerned by the grouping
   * @return
   * @throws Neo4jNoResult
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   * @throws Neo4jBadRequestException
   */
  public static Node groupSingleTag(
      Neo4jAL neo4jAL, String applicationContext, String groupName, List<Node> nodeList)
      throws Neo4jNoResult, Neo4jQueryException, Neo4jBadNodeFormatException,
          Neo4jBadRequestException {

    RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);

    // Timer used to follows the time taken by each steps
    Instant startTimer;
    Instant finishTimer;

    List<Level5Node> affectedLevels = new ArrayList<>();
    startTimer = Instant.now();

    Node oldLevel5Node = null;
    // Retrieve most encountered Level 5
    for (Iterator<Map.Entry<Node, Integer>> it = getLevel5(neo4jAL, nodeList); it.hasNext(); ) {
      Map.Entry<Node, Integer> level5entry = it.next();

      // Get first node. Corresponding to the most frequent one
      if (oldLevel5Node == null) {
        oldLevel5Node = level5entry.getKey();
      }

      // Save the level and their nodes
      Level5Node level = Level5Node.fromNode(neo4jAL, level5entry.getKey());
      level.createLevel5Backup(applicationContext, nodeList);

      affectedLevels.add(level);
    }

    finishTimer = Instant.now();
    neo4jAL.logInfo(
        String.format(
            "Level 5 found, with Id : %d has been identified in %d Milliseconds.",
            oldLevel5Node.getId(), Duration.between(startTimer, finishTimer).toMillis()));

    neo4jAL.logInfo("Creating new level 5 node ... ");
    Instant startLevel5Creation = Instant.now();

    // Get associated Level 4 full name and create the level 5 nodes
    // You cannot get it directly by looking at the relationships because sometimes there are
    // several Level4 linked to the same node.
    String oldFullName = (String) oldLevel5Node.getProperty(Level5Node.getFullNameProperty());
    String[] splitArr = oldFullName.split("##");
    String level4FullName = String.join("##", Arrays.copyOf(splitArr, splitArr.length - 1));

    String forgedLevel4 =
        String.format(
            "MATCH (o:%1$s:`%2$s`) WHERE o.%3$s CONTAINS '%4$s' RETURN o as node;",
            IMAGING_LEVEL4_LABEL,
            applicationContext,
            Level5Node.getFullNameProperty(),
            level4FullName);

    Result resLevel4 = neo4jAL.executeQuery(forgedLevel4);

    if (!resLevel4.hasNext()) {
      Neo4jNoResult err =
          new Neo4jNoResult(
              "Cannot find Level 4 node. Aborting grouping operation for tag :" + groupName,
              forgedLevel4,
              ERROR_PREFIX + "GROT1");
      neo4jAL.logError("Cannot find level4.", err);
      throw err;
    }
    Node level4Node = (Node) resLevel4.next().get("node");

    // Forge the name of the level by removing the tag identifier
    String forgedName = groupName.replace(getLevelPrefix(), "");

    // Merge new Level 5 and labelled them with application's name
    String forgedLabel = Level5Node.getLabel() + ":`" + applicationContext + "`";
    String forgedFindLevel =
        String.format(
            "MATCH (o:%1$s) WHERE o.%2$s='%3$s' RETURN o as node;",
            forgedLabel, Level5Node.getNameProperty(), forgedName);

    Node newLevelNode = null;
    Result result = neo4jAL.executeQuery(forgedFindLevel);
    if (result.hasNext()) {
      // Module with same name was found, and results will be merge into it
      newLevelNode = (Node) result.next().get("node");
    } else {
      // Create a new module
      Label applicationLabel = Label.label(applicationContext);
      // Forge properties
      String fullName = level4FullName + "##" + GENERATED_LEVEL_IDENTIFIER + forgedName;
      String color = (String) oldLevel5Node.getProperty(Level5Node.getColorProperty());
      Long count = ((Integer) nodeList.size()).longValue();
      String shade = (String) oldLevel5Node.getProperty(Level5Node.getShadeProperty());

      Level5Node newLevel =
          new Level5Node(neo4jAL, forgedName, false, true, fullName, color, 5L, count, shade);
      newLevelNode = newLevel.createNode();
      newLevelNode.addLabel(applicationLabel);
    }

    // Link new level to Level 4
    level4Node.createRelationshipTo(newLevelNode, aggregatesRel);
    Instant finishLevel5Creation = Instant.now();
    neo4jAL.logInfo(
        String.format(
            "Level 5 was created in %d Milliseconds.",
            Duration.between(startLevel5Creation, finishLevel5Creation).toMillis()));

    neo4jAL.logInfo("Linking the objects selected to the new Level 5 node created... ");
    startTimer = Instant.now();

    // Delete old relationships, to not interfere with the new level
    for (Node n : nodeList) {
      // Find and Delete Old Relationships
      for (Relationship relN : n.getRelationships(Direction.INCOMING, aggregatesRel)) {
        relN.delete();
      }

      // Relink to new level
      newLevelNode.createRelationshipTo(n, aggregatesRel);
      // Change the level name to the new one of each node
      n.setProperty(IMAGING_OBJECT_LEVEL, forgedName);
    }

    finishTimer = Instant.now();
    neo4jAL.logInfo(
        String.format(
            "%d Objects were linked to the new Level in %d Milliseconds.",
            nodeList.size(), Duration.between(startTimer, finishTimer).toMillis()));

    // Refresh Level connections
    startTimer = Instant.now();
    neo4jAL.logInfo("Refreshing the new level relationships and recount elements.");

    LevelsUtils.refreshLevel5(neo4jAL, applicationContext, newLevelNode); // refresh new level
    affectedLevels.forEach(
        x -> {
          try {
            LevelsUtils.refreshLevel5(
                neo4jAL, applicationContext, x.getNode()); // refresh old levels
          } catch (Neo4jQueryException | Neo4jNoResult e) {
            neo4jAL.logInfo(
                String.format(
                    "An error occurred trying to refresh level with name '%s'.", x.getName()));
          }
        });

    finishTimer = Instant.now();
    neo4jAL.logInfo(
        String.format(
            "Refreshed levels in %d Milliseconds.",
            Duration.between(startTimer, finishTimer).toMillis()));

    neo4jAL.logInfo("Now refreshing abstract levels..");
    LevelsUtils.refreshAllAbstractLevel(neo4jAL, applicationContext);

    return newLevelNode;
  }

  /**
   * Group all the level present in an application
   *
   * @param neo4jAL Neo4j access Layer
   * @param applicationContext Name of the Application concerned by the merge
   * @return
   * @throws Neo4jQueryException
   */
  public static List<Node> groupAllLevels(Neo4jAL neo4jAL, String applicationContext)
      throws Neo4jQueryException {
    Map<String, List<Node>> groupMap = new HashMap<>();

    neo4jAL.logInfo("Starting Demeter level 5 grouping...");

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

    List<Node> resNodes = new ArrayList<>();

    // Build a level 5 and attach the node list
    for (Map.Entry<String, List<Node>> entry : groupMap.entrySet()) {
      String groupName = entry.getKey();
      List<Node> nodeList = entry.getValue();

      if (nodeList.isEmpty()) continue;

      try {
        Node n = groupSingleTag(neo4jAL, applicationContext, groupName, nodeList);
        resNodes.add(n);
      } catch (Exception
          | Neo4jNoResult
          | Neo4jBadNodeFormatException
          | Neo4jBadRequestException err) {
        neo4jAL.logError(
            "An error occurred trying to create Level 5 for nodes with tags : " + groupName, err);
      }
    }

    // Clean residual tags
    clean(neo4jAL, applicationContext);

    return resNodes;
  }

  /**
   * Group demeter levels in every applications
   * @param neo4jAL
   * @return
   * @throws Neo4jQueryException
   */
  public static List<Node> groupInAllApplications(Neo4jAL neo4jAL) throws Neo4jQueryException {
    String applicationReq =
        "MATCH (o:Object) WHERE EXISTS (o.Tags) AND any(x in o.Tags WHERE x CONTAINS $tagPrefix) "
            + "RETURN DISTINCT [ x in LABELS(o) WHERE NOT x='Object'][0] as application;";
    Map<String, Object> params = Map.of("tagPrefix", getLevelPrefix());

    Result res = neo4jAL.executeQuery(applicationReq, params);
    List<Node> fullResults = new ArrayList<>();

    // Parse all the application
    while (res.hasNext()) {
      fullResults.addAll(groupAllLevels(neo4jAL, (String) res.next().get("application")));
    }

    return fullResults;
  }
}
