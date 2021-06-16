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

package com.castsoftware.demeter.controllers.grouping.modules;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.config.UserConfiguration;
import com.castsoftware.demeter.controllers.grouping.AGrouping;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.database.Neo4jTypeManager;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.imaging.ModuleNode;
import org.neo4j.graphdb.*;

import java.util.*;

public class ModuleGroupController extends AGrouping {

  // Static Imaging nodes

  // Demeter Conf
  private static String GROUP_MODULE_TAG_IDENTIFIER =
      Configuration.getBestOfALl("demeter.prefix.module_group");

  // Static Imaging relationships
  private static final String IMAGING_CONTAINS =
      Configuration.get("imaging.node.module.links.to_objects");

  static {
    if(GROUP_MODULE_TAG_IDENTIFIER == null) {
      GROUP_MODULE_TAG_IDENTIFIER = Configuration.get("demeter.prefix.module_group");
    }
  }

  /**
   * Refresh the links between the modules, and recreate the correct links
   */
  public void refreshModuleLinks()
      throws Neo4jQueryException {
    String forgedToOtherModules =
            String.format("MATCH (n:Module:`%1$s`)--(int)-->(int2)--(l:Module:`%1$s`) WHERE (int:Object OR int:SubObject) AND (int2:Object OR int2:SubObject) AND ID(n)<>ID(l) " +
                    "MERGE (n)-[:References]->(l)", applicationContext);


    Result resTo = neo4jAL.executeQuery(forgedToOtherModules);
    while (resTo.hasNext()) {
      neo4jAL.logInfo(String.format("Got a relation to %s", (String) resTo.next().get("distant")));
    }
  }

  /**
   * Refresh count for modules
   */
  public void moduleRecount() throws Neo4jQueryException {
    // Update the old Level 5 and remove it is there no node linked to it
    String forgedNumConnected =
        String.format(
            "MATCH (n:Module:`%1$s`)-[:Contains]->(o:Object) WITH n as module, COUNT(o) as links " +
                    "SET module.Count=links ",
                applicationContext);

   neo4jAL.executeQuery(forgedNumConnected);
   // remove modules without links anymore
    String removeEmpty =
            String.format(
                    "MATCH (n:Module:`%1$s`) WHERE n.Count=0 DETACH DELETE n",
                    applicationContext);
    neo4jAL.executeQuery(removeEmpty);
  }


  @Override
  public String getTagPrefix() {
    return  Configuration.getBestOfALl("demeter.prefix.module_group");
  }

  @Override
  public void setTagPrefix(String value) throws FileNotFoundException, MissingFileException {
    Configuration.setEverywhere("demeter.prefix.module_group", value);
  }

  @Override
  public void refresh() throws Neo4jQueryException {
    moduleRecount();
    refreshModuleLinks();
  }

  @Override
  public Node group(String groupName, List<Node> nodeList) throws Neo4jQueryException, Neo4jBadRequestException {
    RelationshipType containsRel = RelationshipType.withName(IMAGING_CONTAINS);
    Label moduleLabel = Label.label("Module");

    // Clean the group name by removing the Demeter Prefix
    groupName = groupName.replace(GROUP_MODULE_TAG_IDENTIFIER, "");

    // Assert the application name is not empty
    assert !applicationContext.isEmpty() : "The application name cannot be empty.";


    // Get other modules nodes
    for (Node n : nodeList) {
      // The node is supposed to be linked to only one module
      Iterator<Relationship> relIt = n.getRelationships(Direction.INCOMING, containsRel).iterator();

      if (!relIt.hasNext()) continue;
      Node modNode = relIt.next().getStartNode();
      // Assert that the node is a module
      if (!modNode.hasLabel(moduleLabel)) continue;
    }

    // Get last AIP ID
    String reqID = "Match (o) WHERE EXISTS(o.AipId) RETURN MAX(o.AipId) + 1 as maxId";
    Result resId = neo4jAL.executeQuery(reqID);

    Long maxId = Neo4jTypeManager.getAsLong(resId.next().get("maxId"), 0L);

    // Get num Object + Sub obj

    // Module Creation of the node (Merge existing nodes)
    String reqModule = String.format("MERGE (m:Module:`%1$s` {Type:'module', Color:'rgb(34, 199, 214)', Name:$name }) " +
            "SET m.AipId=$aipId SET m.AlternateDrilldown=true SET m.Count = CASE WHEN EXISTS(m.Count) THEN m.Count + $numItems ELSE $numItems END " +
            "RETURN m as node", applicationContext);
    Map<String, Object> params = Map.of("numItems", new Long(nodeList.size()), "aipId", maxId.toString(), "name", groupName);
    Result resModule = neo4jAL.executeQuery(reqModule, params);
    neo4jAL.logInfo("Debug request :" + reqModule);
    if(!resModule.hasNext()) {
      throw new Neo4jBadRequestException(String.format("The request %s did not produced any result", reqModule), "MODGxGROM1");
    }

    Node module = (Node) resModule.next().get("node");
    // Treat objects
    // Link all the objects tagged to you modules.
    // Treat node in a first pass
    String forgedRequest;
    Map<String, Object> paramsNode;
    for (Node rObject : nodeList) {
      // Link objects
      paramsNode = Map.of("idObj", rObject.getId(), "idModule", module.getId(), "moduleName", groupName);

      String reObj = String.format("MATCH (o:Object:`%s`) WHERE ID(o)=$idObj " +
              "OPTIONAL MATCH (o)<-[r:Contains]-(oldModule:Module) " +
              "SET o.Module = CASE WHEN o.Module IS NULL THEN [$moduleName] ELSE [ x in o.Module WHERE NOT x=oldModule.Name ] + $moduleName END " +
              "DELETE r " +
              "WITH o as obj " +
              "MATCH (newM:Module) WHERE ID(newM)=$idModule " +
              "CREATE (newM)-[:Contains]->(obj) ", applicationContext);

      String subObj = String.format("MATCH (o:Object:`%s`)<-[:BELONGTO]-(j:SubObject) WHERE ID(o)=$idObj " +
              "WITH j " +
              "OPTIONAL MATCH (m:Module)-[rd:Contains]->(j) " +
              "SET j.Module = CASE WHEN j.Module IS NULL THEN [$moduleName] ELSE j.Module + $moduleName END " +
              "DELETE rd " +
              "WITH j " +
              "MATCH (newM:Module) WHERE ID(newM)=$idModule CREATE (newM)-[:Contains]->(j)  ", applicationContext);

      neo4jAL.executeQuery(reObj, paramsNode);
      neo4jAL.executeQuery(subObj, paramsNode);
    }

    return module;
  }

  public ModuleGroupController(Neo4jAL neo4jAL, String applicationContext) {
    super(neo4jAL, applicationContext);

  }

}
