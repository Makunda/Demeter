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

public class ConfigurationNode extends Neo4jObject {
  // Configuration properties
  private static final String LABEL = Configuration.get("neo4j.nodes.t_configuration");
  private static final String INDEX_PROPERTY =
      Configuration.get("neo4j.nodes.t_configuration.index");
  private static final String NAME_PROPERTY = Configuration.get("neo4j.nodes.t_configuration.name");

  private static final String ERROR_PREFIX =
      Configuration.get("neo4j.nodes.t_configuration.error_prefix");

  // Node properties
  private String name;

  public ConfigurationNode(Neo4jAL nal, String name) {
    super(nal);
    this.name = name;
  }

  public static String getLabel() {
    return LABEL;
  }

  public static String getNameProperty() {
    return NAME_PROPERTY;
  }

  /**
   * Create a Configuration Node object from a neo4j node
   *
   * @param neo4jAL Neo4j Access Layer
   * @param node Node associated to the object
   * @return <code>ConfigurationNode</code> the object associated to the node.
   * @throws Neo4jBadNodeFormatException If the conversion from the node failed due to a missing or
   *     malformed property.
   */
  public static ConfigurationNode fromNode(Neo4jAL neo4jAL, Node node)
      throws Neo4jBadNodeFormatException {

    if (!node.hasLabel(Label.label(LABEL))) {
      throw new Neo4jBadNodeFormatException(
          "The node does not contain the correct label. Expected to have : " + LABEL,
          ERROR_PREFIX + "FROMN1");
    }

    try {
      String name = (String) node.getProperty(NAME_PROPERTY);

      // Initialize the node
      ConfigurationNode confn = new ConfigurationNode(neo4jAL, name);
      confn.setNode(node);

      return confn;
    } catch (NotFoundException | NullPointerException | ClassCastException e) {
      throw new Neo4jBadNodeFormatException(
          LABEL + " instantiation from node.", ERROR_PREFIX + "FROMN2");
    }
  }

  public static List<ConfigurationNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jNoResult {
    Label label = Label.label(LABEL);
    List<ConfigurationNode> returnList = new ArrayList<>();

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

  @Override
  public Node createNode() throws Neo4jBadRequestException, Neo4jNoResult {
    neo4jAL.getLogger().info("Starting create node ");
    String queryDomain =
        String.format("CREATE (p:%s { %s : '%s' }) RETURN p as node;", LABEL, NAME_PROPERTY, name);
    try {
      Result res = neo4jAL.executeQuery(queryDomain);
      Node n = (Node) res.next().get("node");
      this.setNode(n);
      neo4jAL.getLogger().info("End create node req : " + queryDomain);
      return n;
    } catch (Neo4jQueryException e) {
      throw new Neo4jBadRequestException(
          LABEL + " node creation failed", queryDomain, e, ERROR_PREFIX + "CRN1");
    } catch (NoSuchElementException | NullPointerException e) {
      throw new Neo4jNoResult(
          LABEL + "node creation failed", queryDomain, e, ERROR_PREFIX + "CRN2");
    }
  }
}
