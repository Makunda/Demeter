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

package com.castsoftware.demeter.utils;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.imaging.Level5Node;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;

public class LevelsUtils {

  // Imaging Conf
  private static final String IMAGING_LEVEL1_LABEL = Configuration.get("imaging.node.level1.label");
  private static final String IMAGING_LEVEL2_LABEL = Configuration.get("imaging.node.level2.label");
  private static final String IMAGING_LEVEL3_LABEL = Configuration.get("imaging.node.level3.label");
  private static final String IMAGING_LEVEL4_LABEL = Configuration.get("imaging.node.level4.label");
  private static final String IMAGING_LEVEL5_LABEL = Configuration.get("imaging.node.level5.label");

  private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
  private static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");
  private static final String IMAGING_OBJECT_LEVEL = Configuration.get("imaging.node.object.level");
  private static final String IMAGING_AGGREGATES =
      Configuration.get("imaging.node.level_nodes.links");
  private static final String IMAGING_LEVEL_REFERENCES =
      Configuration.get("imaging.node.level_nodes.references");

  /**
   * Get the label associated with the level number provided
   *
   * @param levelNumber
   * @return the label of the corresponding label
   */
  public static String getLevelLabelByNumber(int levelNumber) {
    switch (levelNumber) {
      case 1:
        return IMAGING_LEVEL1_LABEL;
      case 2:
        return IMAGING_LEVEL2_LABEL;
      case 3:
        return IMAGING_LEVEL3_LABEL;
      case 4:
        return IMAGING_LEVEL4_LABEL;
      case 5:
        return IMAGING_LEVEL5_LABEL;
      default:
        throw new IllegalArgumentException(
            String.format("No level corresponds to number %d.", levelNumber));
    }
  }

  /**
   * Refresh all levels connections, from Level 4 to Level 1
   *
   * @param neo4jAL Neo4j Access Layer
   * @param applicationContext The application concerned by the operation
   */
  public static void refreshAllAbstractLevel(Neo4jAL neo4jAL, String applicationContext)
      throws Neo4jQueryException {
    RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);

    if(applicationContext.contains("-")) {
      applicationContext = String.format("`%s`", applicationContext);
    }

    // Refresh from level 4 to  level 1
    for (int i = 4; i > 1; i--) {
      String labelN = getLevelLabelByNumber(i);
      String labelN0 = getLevelLabelByNumber(i - 1);

      // Get nodes for this specific level
      String getNodeReq =
          String.format("MATCH (l:%1$s:%2$s) RETURN l as level", labelN, applicationContext);
      Result result = neo4jAL.executeQuery(getNodeReq);

      while (result.hasNext()) {
        Node ln = (Node) result.next().get("level");

        String forgedToLevels =
            String.format(
                "MATCH (level:%1$s:%2$s)-[:%3$s]->(:%4$s:%2$s)-->(o:%4$s:%2$s)<-[:%3$s]-(l:%1$s) WHERE ID(level)=%5$d RETURN DISTINCT l as level;",
                labelN, applicationContext, IMAGING_AGGREGATES, labelN0, ln.getId());

        String forgedFromLevel =
            String.format(
                "MATCH (level:%1$s:%2$s)-[:%3$s]->(:%4$s:%2$s)<--(o:%4$s:%2$s)<-[:%3$s]-(l:%1$s) WHERE ID(level)=%5$d RETURN DISTINCT l as level;",
                labelN, applicationContext, IMAGING_AGGREGATES, labelN0, ln.getId());

        String deleteOldRelations =
            String.format(
                "MATCH (level:%1$s:%2$s)-[r:%3$s]-(:%1s:%2$s) WHERE ID(level)=%4$d DELETE r",
                labelN, applicationContext, IMAGING_AGGREGATES, ln.getId());

        // Remove old relations
        neo4jAL.executeQuery(deleteOldRelations);

        // Get others levels linked to this level - Outgoing level
        Result resTo = neo4jAL.executeQuery(forgedToLevels);
        while (resTo.hasNext()) {
          Node resToNode = (Node) resTo.next().get("level");
          if (resToNode.getId() == ln.getId()) continue; // Ignore self relationships

          ln.createRelationshipTo(resToNode, aggregatesRel);
          ;
        }

        // Get others levels linked to this level - Incoming level
        Result resFrom = neo4jAL.executeQuery(forgedFromLevel);
        while (resFrom.hasNext()) {
          Node resFromNode = (Node) resFrom.next().get("level");
          if (resFromNode.getId() == ln.getId()) continue; // Ignore self relationships

          resFromNode.createRelationshipTo(ln, aggregatesRel);
        }
      }
    }
  }

  /**
   * Refresh the links inter modules
   *
   * @param neo4jAL
   * @param applicationContext
   * @param nodeLevel
   * @return
   * @throws Neo4jQueryException
   */
  public static Node refreshLevelLinks5(Neo4jAL neo4jAL, String applicationContext, Node nodeLevel)
      throws Neo4jQueryException {
    RelationshipType referenceRel = RelationshipType.withName(IMAGING_LEVEL_REFERENCES);

    // Remove actual level relationships
    for (Relationship rel : nodeLevel.getRelationships(referenceRel)) {
      rel.delete();
    }

    if(applicationContext.contains("-")) {
      applicationContext = String.format("`%s`", applicationContext);
    }

    String forgedToOtherLevel5 =
        String.format(
            "MATCH (inil:%1$s:%2$s)-[:%3$s]->(inio:%4$s:%2$s)-->(o:%4$s:%2$s)<-[:%3$s]-(l:%1$s) WHERE ID(inil)=%5$s AND inil.%6$s=inio.%7$s  AND l.%6$s=o.%7$s RETURN DISTINCT l as level;",
            Level5Node.getLabel(),
            applicationContext,
            IMAGING_AGGREGATES,
            IMAGING_OBJECT_LABEL,
            nodeLevel.getId(),
            Level5Node.getNameProperty(),
            IMAGING_OBJECT_LEVEL);
    // List incoming level 5
    String forgedFromOtherLevel5 =
        String.format(
            "MATCH (inil:%1$s:%2$s)-[:%3$s]->(inio:%4$s:%2$s)<--(o:%4$s:%2$s)<-[:%3$s]-(l:%1$s) WHERE ID(inil)=%5$s AND inil.%6$s=inio.%7$s  AND l.%6$s=o.%7$s RETURN DISTINCT l as level;",
            Level5Node.getLabel(),
            applicationContext,
            IMAGING_AGGREGATES,
            IMAGING_OBJECT_LABEL,
            nodeLevel.getId(),
            Level5Node.getNameProperty(),
            IMAGING_OBJECT_LEVEL);

    Result resTo = neo4jAL.executeQuery(forgedToOtherLevel5);
    while (resTo.hasNext()) {
      Node resToNode = (Node) resTo.next().get("level");
      if (resToNode.getId() == nodeLevel.getId()) continue; // Ignore self relationships

      nodeLevel.createRelationshipTo(resToNode, referenceRel);
      ;
    }

    Result resFrom = neo4jAL.executeQuery(forgedFromOtherLevel5);
    while (resFrom.hasNext()) {
      Node resFromNode = (Node) resFrom.next().get("level");
      if (resFromNode.getId() == nodeLevel.getId()) continue; // Ignore self relationships

      resFromNode.createRelationshipTo(nodeLevel, referenceRel);
    }

    return nodeLevel;
  }

  /**
   * Refresh the count parameter for a level ( Counting objects linked to it )
   *
   * @param neo4jAL Neo4j access layer
   * @param applicationContext Application concerned by the change
   * @param levelNode Level node necessitating a
   * @return The Node updated
   * @throws Neo4jQueryException
   */
  public static Node refreshLevelCount5(Neo4jAL neo4jAL, String applicationContext, Node levelNode)
      throws Neo4jQueryException {
    // Update the old Level 5 and remove it is there no node linked to it

    String forgedNumConnected =
        String.format(
            "MATCH (n:`%1$s`:%2$s)-[:%3$s]->(o:%4$s) WHERE ID(n)=%5$s AND n.%6$s=o.%7$s RETURN COUNT(o) as countNode;",
            applicationContext,
            Level5Node.getLabel(),
            IMAGING_AGGREGATES,
            IMAGING_OBJECT_LABEL,
            levelNode.getId(),
            Level5Node.getNameProperty(),
            IMAGING_OBJECT_LEVEL);

    Result resNumConnected = neo4jAL.executeQuery(forgedNumConnected);

    Long numLeft = 0L;
    if (resNumConnected.hasNext()) {
      numLeft = (Long) resNumConnected.next().get("countNode");
    }

    // Delete the oldLevel node if it's empty
    if (numLeft == 0) {
      // Detach
      for (Relationship rel : levelNode.getRelationships()) {
        rel.delete();
      }
      // Delete
      levelNode.delete();
      neo4jAL.logInfo(
          String.format(
              "Level with ID '%d' had no more relationships with objects and was deleted.",
              levelNode.getId()));
      return null;
    } else {
      // Update count property
      levelNode.setProperty(Level5Node.getCountProperty(), numLeft);
      neo4jAL.logInfo(
          "Level still has " + numLeft + " relationships with objects and will not be deleted.");
      return levelNode;
    }
  }

  /**
   * Refresh both links and count in a level 5 node
   *
   * @param neo4jAL Neo4JAccess Layer
   * @param applicationContext Name of the application you're working on
   * @param node Level 5 node to treat
   * @return The Node
   * @throws Neo4jQueryException
   */
  public static Node refreshLevel5(Neo4jAL neo4jAL, String applicationContext, Node node)
      throws Neo4jQueryException {
    Node n = refreshLevelCount5(neo4jAL, applicationContext, node);
    if (n == null) return null; // Stop the refresh if the node was deleted
    return refreshLevelLinks5(neo4jAL, applicationContext, node);
  }
}
