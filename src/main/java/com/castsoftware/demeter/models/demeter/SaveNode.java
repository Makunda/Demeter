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

package com.castsoftware.demeter.models.demeter;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.Neo4jObject;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class SaveNode extends Neo4jObject {

  private static final String LABEL = Configuration.get("save.node.label");
  private static final String NAME_PROPERTY = Configuration.get("save.node.name");
  private static final String APPLICATION_PROPERTY = Configuration.get("save.node.application");
  private static final String CREATION_PROPERTY = Configuration.get("save.node.creation");

  private static final String RELATION_TO_OPERATIONS =
      Configuration.get("save.operation.node.links.to_save");
  private static final String ERROR_PREFIX = Configuration.get("save.node.error_prefix");

  private String name;
  private String application;
  private String creation;

  /***
   * Constructor
   * @param neo4jAL Neo4j access layer
   * @param name Name of the save
   * @param application Application concerned by the save
   * @param creation Creation date
   */
  public SaveNode(Neo4jAL neo4jAL, String name, String application, String creation) {
    super(neo4jAL);
    this.name = name;
    this.application = application;
    this.creation = creation;
  }

  /**
   * Create a Save node object from Neo4j Node
   *
   * @param neo4jAL Neo4j Access Layer
   * @param node Node to convert
   * @return The save node created from Neo4j node.
   * @throws Neo4jBadNodeFormatException The node provided is not in a correct format
   */
  public static SaveNode fromNode(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException {
    if (!node.hasLabel(Label.label(LABEL))) {
      throw new Neo4jBadNodeFormatException(
          String.format(
              "The node with Id '%d' does not contain the correct label. Expected to have : %s",
              node.getId(), LABEL),
          ERROR_PREFIX + "FROMN1");
    }

    try {
      String name = (String) node.getProperty(NAME_PROPERTY);
      String application = (String) node.getProperty(APPLICATION_PROPERTY);
      String creation = (String) node.getProperty(CREATION_PROPERTY);

      // Initialize the node
      SaveNode sn = new SaveNode(neo4jAL, name, application, creation);
      sn.setNode(node);

      return sn;
    } catch (NotFoundException | NullPointerException | ClassCastException e) {
      throw new Neo4jBadNodeFormatException(
          LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
    }
  }

  /**
   * Get all the Save node present in the database.
   *
   * @param neo4jAL
   * @return
   * @throws Neo4jNoResult
   */
  public static List<SaveNode> getAllSaveNodes(Neo4jAL neo4jAL) throws Neo4jNoResult {
    Label label = Label.label(LABEL);
    List<SaveNode> returnList = new ArrayList<>();

    Node n = null;
    for (ResourceIterator<Node> it = neo4jAL.getTransaction().findNodes(label); it.hasNext(); ) {
      try {
        returnList.add(fromNode(neo4jAL, it.next()));
      } catch (NoSuchElementException | NullPointerException | Neo4jBadNodeFormatException e) {
        throw new Neo4jNoResult(
            LABEL + "nodes retrieving by application name failed",
            "findQuery",
            e,
            ERROR_PREFIX + "GANA1");
      }
    }

    return returnList;
  }

  public String getName() {
    return name;
  }

  public String getApplication() {
    return application;
  }

  public String getCreation() {
    return creation;
  }

  @Override
  public Node createNode() {
    Label label = Label.label(LABEL);
    Node n = neo4jAL.getTransaction().createNode(label);

    n.setProperty(NAME_PROPERTY, this.name);
    n.setProperty(APPLICATION_PROPERTY, this.application);
    n.setProperty(CREATION_PROPERTY, this.creation);

    this.setNode(n);
    return n;
  }

  /**
   * Delete the node and all the operations nodes attached to this Save node
   *
   * @throws Neo4jBadRequestException
   * @throws Neo4jNoResult
   * @throws Neo4jQueryException
   */
  @Override
  public void deleteNode() throws Neo4jBadRequestException, Neo4jNoResult, Neo4jQueryException {
    Node n = getNode();

    // Delete attached operations
    Node opN = null;
    for (Relationship rel :
        n.getRelationships(Direction.INCOMING, RelationshipType.withName(RELATION_TO_OPERATIONS))) {
      opN = rel.getStartNode();
      rel.delete(); // Delete relationship
      opN.delete(); // Delete operation node
    }

    super.deleteNode(); // Delete the Demeter save node
  }

  /**
   * Execute the save
   *
   * @return The number of nodes affected by the save
   */
  public int execute() throws Neo4jQueryException, Neo4jNoResult, Neo4jBadNodeFormatException {
    Node n = getNode();
    int affectedNode = 0;

    // Get operations nodes
    Node node = null;
    OperationNode opN;
    for (Relationship rel :
        n.getRelationships(Direction.INCOMING, RelationshipType.withName(RELATION_TO_OPERATIONS))) {
      node = rel.getStartNode();
      opN = OperationNode.fromNode(neo4jAL, node);
      affectedNode += opN.execute(this.application);
    }

    return affectedNode;
  }
}
