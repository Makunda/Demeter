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

package com.castsoftware.demeter.controllers.grouping.architectures;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.*;

import java.util.*;

public class MicroserviceController extends ArchitectureGroupController {

  public MicroserviceController(Neo4jAL neo4jAL, String applicationContext) {
    super(neo4jAL, applicationContext);
  }

  public static String getPrefix() {
    return Configuration.getBestOfALl("demeter.prefix.microservice_group");
  }

  // Override the groups
  @Override
  public Map<String, List<Node>> getGroupList() throws Neo4jQueryException {
    // Get the list of nodes prefixed by dm_tag
    String forgedTagRequest =
        String.format(
            "MATCH (o:`%1$s`) WHERE any( x in o.Tags WHERE x CONTAINS $tagPrefix)  "
                + "WITH o, [x in o.Tags WHERE x CONTAINS $tagPrefix] as g "
                + "RETURN DISTINCT o as node, g as group;",
            applicationContext);
    Map<String, Object> params = Map.of("tagPrefix", getTagPrefix());

    Map<String, List<Node>> groupMap = new HashMap<>();
    Result res = neo4jAL.executeQuery(forgedTagRequest, params);
    // Build the map for each group as <Tag, Node list>
    while (res.hasNext()) {
      Map<String, Object> resMap = res.next();
      List<String> groupList = (List<String>) resMap.get("group");
      Node node = (Node) resMap.get("node");

      for (String g : groupList) {
        // Add to  the specific group
        if (!groupMap.containsKey(g)) {
          groupMap.put(g, new ArrayList<>());
        }
        groupMap.get(g).add(node);
      }
    }

    neo4jAL.logInfo(
        String.format(
            "%d module groups (Prefix: %s) were identified.", groupMap.size(), getTagPrefix()));
    for (String l : groupMap.keySet()) {
      neo4jAL.logInfo(String.format("Group name : %s and size : %d", l, groupMap.get(l).size()));
    }

    return groupMap;
  }


  /**
   * Get the objects under the architecture model
   *
   * @param neo4jAL
   * @param idArchi
   * @param applicationContext
   * @return
   * @throws Neo4jQueryException
   */
  public List<Node> getObjectsUnderArchitectureModel(
      Neo4jAL neo4jAL, Long idArchi, String applicationContext) throws Neo4jQueryException {
    // Match
    String req =
        String.format(
            "MATCH (n:ArchiModel:`%1$s`)-->(:Subset)-->(o:Object) "
                + "WHERE ID(n)=$id "
                + "RETURN DISTINCT o as node;",
            applicationContext);
    Map<String, Object> params = Map.of("id", idArchi);

    List<Node> nodes = new ArrayList<>();
    Result res = neo4jAL.executeQuery(req, params);
    while (res.hasNext()) {
      nodes.add((Node) res.next().get("node"));
    }

    return nodes;
  }

  public void extractOneMicroservice(String architecturePrefix, Long idStartingNode)
      throws Neo4jQueryException, Neo4jBadRequestException, Neo4jBadNodeFormatException {

    // Get starting node and ensure we're starting from a object
    Node startingNode = neo4jAL.getNodeById(idStartingNode);
    if (!startingNode.hasLabel(Label.label("Object"))) {
      throw new Neo4jBadNodeFormatException(
          "Can only process Objects as starting point", "MICxEOM01");
    }

    Long processed = 0L;
    neo4jAL.logInfo("Processing node: " + startingNode.getProperty("Name"));
    // forge name
    String uniqueArchi = String.format("%s-%d", architecturePrefix, 0);
    String microserviceName = ((String) startingNode.getProperty("Name")).replace("Controller", "");
    String microserviceFullName = uniqueArchi + " " + microserviceName + " Microservice$";

    // Flag
    List<Node> nodeList = flagNode(microserviceFullName, startingNode, Direction.OUTGOING);
    // processed += flagNode(microserviceFullName, con, Direction.INCOMING).size();

    // Extract
    neo4jAL.logInfo("Grouping launched for : " + startingNode.getProperty("Name"));
    this.launchWithoutClean();

    // Clean specific tag
    String removeTag =
        String.format(
            "MATCH(o:Object:`%1$s`) WHERE EXISTS(o.Tags) SET o.Tags=[x in o.Tags WHERE NOT x  STARTS WITH $tagName]",
            applicationContext);
    Map<String, Object> paramsTag = Map.of("tagName", "$a_" + microserviceFullName);
    neo4jAL.logInfo("Cleaning launched for : " + startingNode.getProperty("Name"));
    neo4jAL.executeQuery(removeTag, paramsTag);

    neo4jAL.logInfo("Operation finished for : " + startingNode.getProperty("Name"));
  }

  private List<Node> flagNode(String microserviceFullName, Node startingNode, Direction direction)
      throws Neo4jQueryException {
    long processedOne = 0L;
    neo4jAL.logInfo(
        String.format("Processing %s controller.", startingNode.getProperty("Name")));

    List<Node> visited = new ArrayList<>();
    List<Long> visitedID = new ArrayList<>();
    Stack<Node> toVisit = new Stack<>();

    // Get neighbors of controllers
    for (Relationship r : startingNode.getRelationships()) {
      toVisit.add(r.getOtherNode(startingNode));
    }
    visitedID.add(startingNode.getId());

    // Init to visit To bottom
    // Apply prop on controller
    String tag = "$a_" + microserviceFullName + "Entry";
    String reqArchi =
        String.format(
            "MATCH (obj:Object:`%1$s`) WHERE ID(obj)=$Id SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [$tag] ELSE obj.Tags + $tag END;",
            applicationContext);
    Map<String, Object> params = Map.of("Id", startingNode.getId(), "tag", tag);
    neo4jAL.executeQuery(reqArchi, params);

    // Parse relationship outgoing
    while (!toVisit.empty()) {
      Node treat = toVisit.pop();
      // Ignore if visited
      if (visitedID.contains(treat.getId())) continue;

      // Add to visited nodes
      visitedID.add(treat.getId());
      visited.add(treat);
      // flag for microservice

      // Flag with architecture
      if (treat.hasLabel(Label.label("Object")) && treat.hasProperty("Level")) {
        tag = "$a_" + microserviceFullName + treat.getProperty("Level");
        reqArchi =
            String.format(
                "MATCH (obj:Object:`%1$s`) WHERE ID(obj)=$Id "
                    + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [$tag] ELSE obj.Tags + $tag END;",
                applicationContext);
        params = Map.of("Id", treat.getId(), "tag", tag);
        neo4jAL.executeQuery(reqArchi, params);

        // Get all linked objects
        // Skip if the node isn't an object or a subObject

        // Add relationships
        for (Relationship rel : treat.getRelationships(direction)) {
          Node out = rel.getOtherNode(treat);
          toVisit.add(out);
        }

        processedOne++;
      }

    }

    neo4jAL.logInfo(
        String.format(
            "IN %s controller : %d", startingNode.getProperty("Name"), processedOne));

    return visited;
  }

  @Override
  public String getTagPrefix() {
    return getPrefix();
  }

  @Override
  public void setTagPrefix(String value) throws FileNotFoundException, MissingFileException {
    setPrefix(value);
  }

  public void extractMicroservice(String architecturePrefix)
      throws Neo4jQueryException, Neo4jBadRequestException {

    // Get all node controllers
    String req =
        String.format(
            "MATCH (obj:Object:`%1$s`) WHERE any([x in obj.Tags WHERE NOT x  STARTS WITH $tagName]) RETURN obj as node ORDER BY obj.Name",
            applicationContext);
    Map<String, Object> reqGather = Map.of("tagName", getPrefix());
    Result res = neo4jAL.executeQuery(req);

    List<Node> candidates = new ArrayList<>();
    while (res.hasNext()) {
      candidates.add((Node) res.next().get("node"));
    }

    neo4jAL.logInfo(String.format("Detected %d candidates", candidates.size()));

    List<String> createdArchiModels = new ArrayList<>();

    long uniqueId = 0L;
    for (Node con : candidates) {
      neo4jAL.logInfo("Processing node: " + con.getProperty("Name"));
      // forge name
      String uniqueArchi = String.format("%s-%d", architecturePrefix, uniqueId);
      String microserviceName = ((String) con.getProperty("Name"));
      String microserviceFullName = uniqueArchi + " " + microserviceName + " Microservice$";

      // Flag
      flagNode(microserviceFullName, con, Direction.OUTGOING).size();
      // processed += flagNode(microserviceFullName, con, Direction.INCOMING).size();
      uniqueId++;

      // Extract
      neo4jAL.logInfo("Grouping launched for : " + con.getProperty("Name"));
      this.launchWithoutClean();

      // Clean specific tag
      String removeTag =
          String.format(
              "MATCH(o:Object:`%1$s`) WHERE EXISTS(o.Tags) SET o.Tags=[x in o.Tags WHERE NOT x  STARTS WITH $tagName]",
              applicationContext);
      Map<String, Object> paramsTag = Map.of("tagName", getPrefix() + microserviceFullName);
      neo4jAL.logInfo("Cleaning launched for : " + con.getProperty("Name"));
      neo4jAL.executeQuery(removeTag, paramsTag);

      createdArchiModels.add(microserviceFullName);
      neo4jAL.logInfo("Operation finished for : " + con.getProperty("Name"));
    }

    refresh();
  }
}
