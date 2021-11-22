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

    String deleteLinks = null;
    String refreshLinks = null;
    String refreshCount = null;

    String deleteEmptyLevel5 =
        String.format(
            "MATCH (l:Level5:`%s`) WHERE NOT (l)-[]->(:Object) DETACH DELETE l",
            applicationContext);

    // Refresh from 1 to 4 levels
    for (int i = 5; i >= 1; i--) {
      try {
        neo4jAL.logInfo(
            String.format("Refreshing level %d in application '%s'...", i, applicationContext));
        // Delete to refresh
        deleteLinks =
            String.format(
                "MATCH (:Level%2$d:`%1$s`)-[r]->(:Level%2$d:`%1$s`) DELETE r",
                applicationContext, i);

        neo4jAL.executeQuery(deleteLinks);

        if (i == 5) {
          refreshLinks =
              String.format(
                  "MATCH (l:Level5:`%1$s`)-[]->(o)-->(o2)<-[]-(l2:Level5:`%1$s`) "
                      + "WHERE ( o:Object OR o:SubObject ) AND ( o2:Object OR o2:SubObject ) AND ID(l)<>ID(l2) "
                      + "MERGE (l)-[:References]->(l2); ",
                  applicationContext);

          refreshCount =
              String.format(
                  "MATCH (l:Level5:`%1$s`)-[]->(o:Object) "
                      + "WITH l, COUNT(DISTINCT o) as objCount "
                      + "SET l.Count=objCount;",
                  applicationContext);
        } else {
          refreshLinks =
              String.format(
                  "MATCH (l:Level%2$d:`%1$s`)-[]->(o)-->(o2)<-[]-(l2:Level%2$d:`%1$s`) "
                      + "WHERE o:Level%3$d AND o2:Level%3$d AND ID(l)<>ID(l2) "
                      + "MERGE (l)-[:References]->(l2); ",
                  applicationContext, i, i + 1);

          refreshCount =
              String.format(
                  "MATCH (l:Level%2$d:`%1$s`)-[]->(lChild:Level%3$d) WHERE EXISTS(lChild.Count) "
                      + "WITH l, SUM(lChild.Count) as objCount "
                      + "SET l.Count=objCount;",
                  applicationContext, i, i + 1);
        }

        neo4jAL.executeQuery(refreshLinks);
        neo4jAL.executeQuery(refreshCount);

        neo4jAL.executeQuery(deleteEmptyLevel5);

      } catch (Exception | Neo4jQueryException err) {
        neo4jAL.logError(
            String.format(
                "Failed to refresh level '%d' in application '%s'.", i, applicationContext),
            err);
        if (deleteLinks != null) neo4jAL.logError("Delete Links : " + deleteLinks);
        if (refreshLinks != null) neo4jAL.logError("Refresh Links : " + refreshLinks);
        if (refreshCount != null) neo4jAL.logError("Refresh Count : " + refreshCount);
        neo4jAL.logError("Delete Empty level 5: " + deleteEmptyLevel5);
        throw err;
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

    String forgedToOtherLevel5 =
        String.format(
            "MATCH (inil:`%1$s`:`%2$s`)-[:%3$s]->(inio:%4$s:`%2$s`)-->(o:`%4$s`:`%2$s`)<-[:%3$s]-(l:`%1$s`) WHERE ID(inil)=%5$s AND inil.%6$s=inio.%7$s  AND l.%6$s=o.%7$s RETURN DISTINCT l as level;",
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
            "MATCH (inil:`%1$s`:`%2$s`)-[:%3$s]->(inio:%4$s:`%2$s`)<--(o:`%4$s`:`%2$s`)<-[:%3$s]-(l:`%1$s`) WHERE ID(inil)=%5$s AND inil.%6$s=inio.%7$s  AND l.%6$s=o.%7$s RETURN DISTINCT l as level;",
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
            "MATCH (n:`%1$s`:`%2$s`)-[:%3$s]->(o:`%4$s`) WHERE ID(n)=%5$s AND n.%6$s=o.%7$s RETURN COUNT(o) as countNode;",
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
