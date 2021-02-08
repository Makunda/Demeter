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

package com.castsoftware.demeter.controllers.grouping;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.config.UserConfiguration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.imaging.ModuleNode;
import org.neo4j.graphdb.*;

import java.util.*;

public class ModuleGroupController {

  // Static Imaging nodes

  private static final String IMAGING_MODULE_LABEL = Configuration.get("imaging.node.module.label");
  private static final String IMAGING_MODULE_NAME = Configuration.get("imaging.node.module.name");
  private static final String IMAGING_OBJECT_TAGS =
      Configuration.get("imaging.link.object_property.tags");

  // Demeter Conf
  private static String GROUP_MODULE_TAG_IDENTIFIER =
      UserConfiguration.get("demeter.prefix.module_group");

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
   *
   * @param neo4jAL Neo4j access layer
   * @param nodeModule Node to refresh
   * @throws Neo4jQueryException
   */
  public static void refreshModuleLinks(Neo4jAL neo4jAL, Node nodeModule)
      throws Neo4jQueryException {
    String forgedToOtherModules =
        String.format(
            "MATCH (n:Module)--(int)-->(int2)--(l:Module) WHERE ID(n)=%d AND (int:Object OR int:SubObject) AND (int2:Object OR int2:SubObject)  AND n<>l MERGE (n)-[:References]->(l)",
            nodeModule.getId());

    // List incoming modules
    String forgedFromOtherModules =
        String.format(
            "MATCH (n:Module)<--(int)--(int2)--(l:Module) WHERE ID(n)=%d AND (int:Object OR int:SubObject) AND (int2:Object OR int2:SubObject)  AND n<>l  MERGE (n)<-[:References]-(l)",
            nodeModule.getId());

    Result resTo = neo4jAL.executeQuery(forgedToOtherModules);
    while (resTo.hasNext()) {
      neo4jAL.logInfo(String.format("Got a relation to %s", (String) resTo.next().get("distant")));
    }

    Result resFrom = neo4jAL.executeQuery(forgedFromOtherModules);
    while (resFrom.hasNext()) {
      neo4jAL.logInfo(String.format("Got a relation from %s", (String) resFrom.next().get("coming")));
    }

  }

  /**
   * Refresh count for modules
   *
   * @param neo4jAL Neo4j access layer
   * @param moduleNode Level node necessitating a
   * @return
   * @throws Neo4jQueryException
   */
  public static Node moduleRecount(Neo4jAL neo4jAL, Node moduleNode) throws Neo4jQueryException {
    // Update the old Level 5 and remove it is there no node linked to it
    String forgedNumConnected =
        String.format(
            "MATCH (n:Module)-[:Contains]->(o:Object) WHERE ID(n)=%d RETURN COUNT(o) as countNode;",
             moduleNode.getId());

    Result resNumConnected = neo4jAL.executeQuery(forgedNumConnected);

    Long numLeft = 0L;
    if (resNumConnected.hasNext()) {
      numLeft = (Long) resNumConnected.next().get("countNode");
    }

    // Delete the old module node if it's empty
    if (numLeft == 0) {
      // Detach
      for (Relationship rel : moduleNode.getRelationships()) {
        rel.delete();
      }
      // Delete
      moduleNode.delete();
      neo4jAL.logInfo("Module had no more relationships with objects and was deleted.");
    } else {
      // Update count property
      moduleNode.setProperty(ModuleNode.getCountProperty(), numLeft);
      neo4jAL.logInfo(
          "Module still has " + numLeft + " relationships with objects and will not be deleted.");
    }

    return moduleNode;
  }

  /**
   * Group modules in a specific application
   *
   * @param neo4jAL Neo4j access Layer
   * @param applicationContext Application concerned by the module grouping
   * @param groupName Name of the group to be merge
   * @param nodeList List of nodes
   * @return
   * @throws Neo4jNoResult
   * @throws Neo4jQueryException
   * @throws Neo4jBadNodeFormatException
   * @throws Neo4jBadRequestException
   */
  public static Node groupModule(
      Neo4jAL neo4jAL, String applicationContext, String groupName, List<Node> nodeList)
      throws Neo4jNoResult, Neo4jQueryException, Neo4jBadNodeFormatException,
          Neo4jBadRequestException {

    RelationshipType containsRel = RelationshipType.withName(IMAGING_CONTAINS);
    Label moduleLabel = Label.label(IMAGING_MODULE_LABEL);

    // Clean the group name by removing the Demeter Prefix
    groupName = groupName.replace(GROUP_MODULE_TAG_IDENTIFIER, "");

    // Assert the application name is not empty
    assert !applicationContext.isEmpty() : "The application name cannot be empty.";
    neo4jAL.logInfo(
        nodeList + " Potential candidates for grouping on module with name : " + groupName);

    // Get other modules nodes
    Set<Node> affectedModules = new HashSet<>();
    for (Node n : nodeList) {
      // The node is supposed to be linked to only one module
      Iterator<Relationship> relIt = n.getRelationships(Direction.INCOMING, containsRel).iterator();

      if (!relIt.hasNext()) continue;
      Node modNode = relIt.next().getStartNode();
      // Assert that the node is a module
      if (!modNode.hasLabel(moduleLabel)) continue;

      // Add to set
      affectedModules.add(modNode);
    }

    // Backup Modules
//    for (Node mod : affectedModules) {
//      ModuleNode nodeMode = ModuleNode.fromNode(neo4jAL, mod);
//      nodeMode.createBackup(applicationContext, nodeList);
//    }

    // Get last AIP ID
    String reqID = "Match (o) WHERE EXISTS(o.AipId) RETURN MAX(o.AipId) + 1 as maxId";
    Result resId = neo4jAL.executeQuery(reqID);

    Long maxId;
    if(resId.hasNext()) {

    maxId = (Long) resId.next().get("maxId");
  } else {
    maxId = 0L;
  }

  // Get num Object + Sub obj

  // Module Creation of the node
  String reqModule = String.format("MERGE (m:Module:`%1$s` {Type:'module',Count:$numItems, AipId: $aipId, Color:'rgb(34, 199, 214)', Name:$name}) RETURN m as node", applicationContext);
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

      String reObj = String.format("MATCH (o:Object:`%s`)<-[r:Contains]-(oldModule:Module) " +
              "WHERE ID(o)=$idObj SET o.Module = CASE WHEN o.Module IS NULL THEN [$moduleName] ELSE [ x in o.Module WHERE NOT x=oldModule.Name ] + $moduleName END DELETE r " +
              "WITH o as obj " +
              "MATCH (newM:Module) WHERE ID(newM)=$idModule " +
              "CREATE (newM)-[:Contains]->(obj) ", applicationContext);

      String subObj = String.format("MATCH (o:Object:`%s`)<-[:BELONGTO]-(j:SubObject) WHERE ID(o)=$idObj " +
              "WITH j " +
              "MATCH (m:Module)-[rd:Contains]->(j) " +
              "SET j.Module = CASE WHEN j.Module IS NULL THEN [$moduleName] ELSE j.Module + $moduleName END " +
              "DELETE rd " +
              "WITH j " +
              "MATCH (newM:Module) WHERE ID(newM)=$idModule CREATE (newM)-[:Contains]->(j)  ", applicationContext);


      neo4jAL.executeQuery(reObj, paramsNode);
      neo4jAL.logInfo("Executed obj for node with id : "+ rObject.getId());
      neo4jAL.executeQuery(subObj, paramsNode);
      neo4jAL.logInfo("Executed subObj for node with id : "+ rObject.getId());
    }

    // Count
    try {
      moduleRecount(neo4jAL, module);
      refreshModuleLinks(neo4jAL, module);
    } catch (Exception e) {
      neo4jAL.logError("Refresh failed for new module.", e);
    }

    // Update old modules w/ recount
    for (Node mod : affectedModules) {
      try {
        moduleRecount(neo4jAL, mod);
        refreshModuleLinks(neo4jAL, mod);
      } catch (Exception e) {
        neo4jAL.logError("Refresh failed for old module.", e);
      }
    }

    return module;
  }

  /**
   * Group all Module present in an application
   *
   * @param neo4jAL Neo4j Access Layer
   * @param applicationContext Application where the nodes are going to be merged
   * @return The list of new modules created
   * @throws Neo4jQueryException
   */
  public static List<Node> groupAllModules(Neo4jAL neo4jAL, String applicationContext)
      throws Neo4jQueryException {
    Map<String, List<Node>> groupMap = new HashMap<>();

    neo4jAL.logInfo("Starting Demeter module grouping...");

    // Get the list of nodes prefixed by dm_tag
    String forgedTagRequest =
        String.format(
            "MATCH (o:`%1$s`) WHERE any( x in o.%2$s WHERE x CONTAINS '%3$s')  "
                + "WITH o, [x in o.%2$s WHERE x CONTAINS '%3$s'][0] as g "
                + "RETURN o as node, g as group;",
            applicationContext, IMAGING_OBJECT_TAGS, GROUP_MODULE_TAG_IDENTIFIER);

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

    neo4jAL.logInfo(groupMap.size() + " module groups were identified.");
    neo4jAL.logInfo("Request launched : " + forgedTagRequest);

    List<Node> resNodes = new ArrayList<>();

    // Build a level 5 and attach the node list
    for (Map.Entry<String, List<Node>> entry : groupMap.entrySet()) {
      String groupName = entry.getKey();
      List<Node> nodeList = entry.getValue();

      if (nodeList.isEmpty()) continue;

      try {
        neo4jAL.logInfo("# Now processing group with name : " + groupName);
        Node n = groupModule(neo4jAL, applicationContext, groupName, nodeList);
        resNodes.add(n);
      } catch (Exception
          | Neo4jNoResult
          | Neo4jBadNodeFormatException
          | Neo4jBadRequestException err) {
        neo4jAL.logError(
            "An error occurred trying to create Level 5 for nodes with tags : " + groupName, err);
      }
    }

    neo4jAL.logInfo("Demeter module grouping finished.");
    neo4jAL.logInfo("Cleaning tags...");

    // Once the operation is done, remove Demeter tag prefix tags
    String removeTagsQuery =
        String.format(
            "MATCH (o:`%1$s`) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS '%3$s' ] RETURN COUNT(o) as removedTags;",
            applicationContext, IMAGING_OBJECT_TAGS, GROUP_MODULE_TAG_IDENTIFIER);
    Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery);

    if (tagRemoveRes.hasNext()) {
      Long nDel = (Long) tagRemoveRes.next().get("removedTags");
      neo4jAL.logInfo("# " + nDel + " demeter 'module group tags' were removed from the database.");
    }

    neo4jAL.logInfo("Cleaning Done !");

    return resNodes;
  }
}
