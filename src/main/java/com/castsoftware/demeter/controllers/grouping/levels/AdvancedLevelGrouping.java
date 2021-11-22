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
import com.castsoftware.demeter.utils.LevelsUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdvancedLevelGrouping {

  // Demeter Conf
  private static final String GENERATED_LEVEL_IDENTIFIER =
      Configuration.get("demeter.prefix.generated_level_prefix");

  // Class Conf
  private static final String ERROR_PREFIX = "GROCx";

  private final Neo4jAL neo4jAL;
  private final List<String> messageOutput;

  /**
   * Constructor
   *
   * @param neo4jAL Neo4j Access Lauer
   */
  public AdvancedLevelGrouping(Neo4jAL neo4jAL) {
    this.neo4jAL = neo4jAL;
    this.messageOutput = new ArrayList<>();
  }

  /**
   * Create a group with category
   *
   * @param application Name of the application
   * @param category Name of the Category
   * @param name Name of the level 4 & 5
   * @param idList List of node ID
   * @return The levels created
   */
  public List<Node> groupWithCategory(
      String application, String category, String name, List<Long> idList)
      throws Neo4jQueryException, Neo4jNoResult {
    // Create category node on level 3
    Optional<Node> level3 = findLevelByNameAndDepth(application, category, 3);
    if (level3.isEmpty()) {
      String fullName = String.format("Services##Logic Services##%s", category);
      level3 = Optional.of(createNamedLevel(application, category, fullName, 3));
      neo4jAL.logInfo(String.format("A new level 3 with full name %s has been created.", fullName));
    }

    // Create API Node on level 4
    Optional<Node> level4 = findLevelByNameAndDepth(application, name, 4);
    if (level4.isEmpty()) {
      String fullName = String.format("Services##Logic Services##%s##%s", category, name);
      level4 = Optional.of(createNamedLevel(application, name, fullName, 4));
      neo4jAL.logInfo(String.format("A new level 4 with full name %s has been created.", fullName));
    }

    // Link two nodes
    Node level3Node = level3.get();
    Node level4Node = level4.get();

    // Merge relationships
    linkLevelToChild(application, level3Node, level4Node);

    // Find or create level 5
    Optional<Node> level5 = findLevelByNameAndDepth(application, name, 5);
    if (level5.isEmpty()) {
      String fullName = String.format("Services##Logic Services##%1$s##%2$s##%2$s", category, name);
      level5 = Optional.of(createNamedLevel(application, name, fullName, 5));
      neo4jAL.logInfo(String.format("A new level 5 with full name %s has been created.", fullName));
    }

    // Link to level 3
    Node level5Node = level5.get();
    linkLevelToChild(application, level4Node, level5Node);

    // Reassign objects
    reassignObjects(application, level5Node, idList);

    // Refresh levels
    LevelsUtils.refreshAllAbstractLevel(neo4jAL, application);

    return List.of(level3Node, level4Node, level5Node);
  }

  /**
   * Find a level by name and depth
   *
   * @param application Application
   * @param name Name
   * @param depth Depth
   * @return Optional of the node
   * @throws Neo4jQueryException
   */
  private Optional<Node> findLevelByNameAndDepth(String application, String name, int depth)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (level:Level%2$d:`%1$s`) WHERE level.Name=$name "
                + "RETURN level as node LIMIT 1",
            application, depth);
    Result results = neo4jAL.executeQuery(req, Map.of("name", name));
    if (results.hasNext()) {
      Node n = (Node) results.next().get("node");
      return Optional.of(n);
    } else return Optional.empty();
  }

  /**
   * Create a named level node
   *
   * @param application Name of the application
   * @param name Name of the level to create
   * @param fullName Full Name of the level
   * @param depth depth
   * @return
   */
  private Node createNamedLevel(String application, String name, String fullName, int depth)
      throws Neo4jQueryException, Neo4jNoResult {
    List<String> shades =
        List.of(
            "rgb(105,105,105)",
            "rgb(176,196,222)",
            "rgb(176,196,222)",
            "rgb(176,196,222)",
            "rgb(176,196,222)");
    String shadeTax = String.join("##", shades.subList(0, depth));

    String req =
        String.format(
            " MERGE (level:Level%2$d:`%1$s` { "
                + "Concept: false, "
                + "Color: 'rgb(176,196,222)', "
                + "FullName: $fullName, "
                + "Level: $depth, "
                + "Count: 0, "
                + "Shade: $shade, "
                + "Name: $name"
                + " }) RETURN level as node;",
            application, depth);
    Result results =
        neo4jAL.executeQuery(
            req, Map.of("name", name, "fullName", fullName, "depth", depth, "shade", shadeTax));
    if (results.hasNext()) {
      return (Node) results.next().get("node");
    } else
      throw new Neo4jNoResult(
          "Failed to create a named level the request returned no results.",
          req,
          ERROR_PREFIX + "CREAN01");
  }

  /**
   * Link parent node to children
   *
   * @param parent Parent
   * @param children Children
   */
  private void linkLevelToChild(String application, Node parent, Node children)
      throws Neo4jQueryException {
    // Delete old parent relationships
    String deleteParentRel =
        "MATCH (child:`%1$s`)<-[r:Aggregates]-() WHERE ID(child)=$childId " + "DELETE r";
    neo4jAL.executeQuery(deleteParentRel, Map.of("childId", children.getId()));

    // Merge relationships
    String merge =
        String.format(
            "MATCH (parent:`%1$s`) WHERE ID(parent)=$parentId WITH parent "
                + "MATCH (child:`%1$s`) WHERE ID(child)=$childId "
                + "MERGE (parent)-[:Aggregates]->(child)",
            application);
    neo4jAL.executeQuery(merge, Map.of("parentId", parent.getId(), "childId", children.getId()));
  }

  /**
   * Reassign an objects to a new level 5
   *
   * @param application Name of the application
   * @param level5 Level 5 to attach
   * @param idList List of the Object's Id to reconnected
   */
  private void reassignObjects(String application, Node level5, List<Long> idList)
      throws Neo4jQueryException {
    String req =
        String.format(
            "MATCH (level:Level5:`%1$s`) WHERE ID(level)=$levelId "
                + "WITH level "
                + "UNWIND $idList as idObj "
                + "MATCH (o:Object:`%1$s`)<-[r:Aggregates]-() WHERE ID(o)=idObj "
                + "DELETE r "
                + "SET o.Level=level.Name "
                + "MERGE (level)-[:Aggregates]->(o) "
                + "RETURN DISTINCT o as node",
            application);

    Result result = neo4jAL.executeQuery(req, Map.of("levelId", level5.getId(), "idList", idList));
    long count = result.stream().count();
    neo4jAL.logInfo(String.format("%d objects have been reassigned to the correct level 5", count));
  }

  /**
   * Create a group with category
   *
   * @param application Name of the application
   * @param level1 Name of the 1st level
   * @param level2 Name of the 2nd level
   * @param level3 Name of the 3rd level
   * @param level4 Name of the 4th level
   * @param level5 Name of the 5th level
   * @param idList List of node ID
   * @return The levels created
   */
  public List<Node> groupWithTaxonomy(
      String application,
      String level1,
      String level2,
      String level3,
      String level4,
      String level5,
      List<Long> idList)
      throws Neo4jQueryException, Neo4jNoResult {

    // Level 1
    Optional<Node> level1Node = findLevelByNameAndDepth(application, level1, 1);
    if (level1Node.isEmpty()) {
      level1Node = Optional.of(createNamedLevel(application, level1, level1, 1));
      neo4jAL.logInfo(String.format("A new level 1 with full name %s has been created.", level1));
    }

    // Level 2
    Optional<Node> level2Node = findLevelByNameAndDepth(application, level1, 2);
    if (level2Node.isEmpty()) {
      String fullName = String.format("%s##%s", level1, level2);
      level2Node = Optional.of(createNamedLevel(application, level2, fullName, 2));
      neo4jAL.logInfo(String.format("A new level 2 with full name %s has been created.", fullName));
    }

    // Level 3
    Optional<Node> level3Node = findLevelByNameAndDepth(application, level3, 3);
    if (level3Node.isEmpty()) {
      String fullName = String.format("%s##%s##%s", level1, level2, level3);
      level3Node = Optional.of(createNamedLevel(application, level3, fullName, 3));
      neo4jAL.logInfo(String.format("A new level 3 with full name %s has been created.", fullName));
    }

    // Create API Node on level 4
    Optional<Node> level4Node = findLevelByNameAndDepth(application, level4, 4);
    if (level4Node.isEmpty()) {
      String fullName = String.format("%s##%s##%s##%s", level1, level2, level3, level4);
      level4Node = Optional.of(createNamedLevel(application, level4, fullName, 4));
      neo4jAL.logInfo(String.format("A new level 4 with full name %s has been created.", fullName));
    }

    // Level 5
    Optional<Node> level5Node = findLevelByNameAndDepth(application, level5, 5);
    if (level5Node.isEmpty()) {
      String fullName = String.format("%s##%s##%s##%s##%s", level1, level2, level3, level4, level5);
      level5Node = Optional.of(createNamedLevel(application, level5, fullName, 5));
      neo4jAL.logInfo(String.format("A new level 5 with full name %s has been created.", fullName));
    }

    // Merge relationships
    linkLevelToChild(application, level1Node.get(), level2Node.get());
    linkLevelToChild(application, level2Node.get(), level3Node.get());
    linkLevelToChild(application, level3Node.get(), level4Node.get());
    linkLevelToChild(application, level4Node.get(), level5Node.get());

    // Reassign objects
    reassignObjects(application, level5Node.get(), idList);

    // Refresh levels
    LevelsUtils.refreshAllAbstractLevel(neo4jAL, application);

    return List.of(
        level1Node.get(), level2Node.get(), level3Node.get(), level4Node.get(), level5Node.get());
  }
}
