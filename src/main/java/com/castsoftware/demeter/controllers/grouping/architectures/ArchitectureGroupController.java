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
import com.castsoftware.demeter.controllers.grouping.AGrouping;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.file.FileNotFoundException;
import com.castsoftware.demeter.exceptions.file.MissingFileException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.List;
import java.util.Map;

public class ArchitectureGroupController extends AGrouping {

  public ArchitectureGroupController(Neo4jAL neo4jAL, String applicationContext) {
    super(neo4jAL, applicationContext);
  }

  public static String getPrefix() {
    return Configuration.getBestOfALl("demeter.prefix.architecture_group");
  }

  public static void setPrefix(String value) throws FileNotFoundException, MissingFileException {
    Configuration.setEverywhere("demeter.prefix.architecture_group", value);
  }

  /**
   * Remove a submodel in the extensions
   *
   * @param subModelName
   * @return
   */
  public Long deleteSubModel(String subModelName) throws Neo4jQueryException {
    // Get nodes under the subModel
    String reqNodes =
        String.format(
            "MATCH (s:Subset:`%s`)-[r:Contains]->(o:Object) WHERE s.Name=$nameSubset "
                + "SET o.Subset = [ x in o.Subset WHERE NOT x=$nameSubset ] "
                + "DELETE r "
                + "RETURN COUNT(o) as removed",
            applicationContext);
    Map<String, Object> params = Map.of("nameSubset", subModelName);
    Result res = neo4jAL.executeQuery(reqNodes, params);
    Long numRemoved = 0L;
    if (res.hasNext()) {
      numRemoved = (Long) res.next().get("removed");
    }

    // Delete the node
    // Get nodes under the subModel
    String reqDelSubset =
        String.format(
            "MATCH (s:Subset:`%s`) WHERE s.Name=$nameSubset DETACH DELETE s ",
            applicationContext);
    neo4jAL.executeQuery(reqDelSubset, params);

    // Refresh the archi node
    refresh();

    return numRemoved;
  }

  public Long deleteArchi(String subModelName) throws Neo4jQueryException {

    // Get nodes under the subModel
    String reqNodes =
        String.format(
            "MATCH (s:Subset:`%s`)-[r:Contains]->(o:Object) "
                + "SET o.Subset = [ x in o.Subset WHERE NOT x=$nameSubset ] "
                + "DELETE r "
                + "RETURN COUNT(o) as removed",
            applicationContext);
    Map<String, Object> params = Map.of("nameSubset", subModelName);
    Result res = neo4jAL.executeQuery(reqNodes, params);
    Long numRemoved = 0L;

    if (res.hasNext()) {
      numRemoved = (Long) res.next().get("removed");
    }

    // Delete the node
    // Get nodes under the subModel
    String reqDelSubset =
        String.format(
            "MATCH (s:Subset:`%s`) WHERE s.Name=$nameSubset DETACH DELETE s ",
            applicationContext);
    neo4jAL.executeQuery(reqDelSubset, params);

    // Refresh the archi node
    refresh();

    return numRemoved;
  }

  /**
   * Refresh the subsets in the application
   *
   * @throws Neo4jQueryException
   */
  private void refreshSubset() throws Neo4jQueryException {
    String reqRefreshCount =
        String.format(
            "MATCH (s:Subset:`%s`) " +
            "WITH s " +
            "MATCH (s)-[:Contains]->(o:Object) " +
            "WITH s, COUNT(o) as tot SET s.Count=tot RETURN s as subset, tot as total",

            applicationContext);
    Result res = neo4jAL.executeQuery(reqRefreshCount);
    if(res.hasNext()) {
      Map<String, Object> results = res.next();
      Long count = (Long) results.get("total");
      Node n = (Node) results.get("subset");

      if(count == 0L) n.delete();
    }

    String refreshLinkReq =
        String.format("MATCH (n:Subset:`%1$s`)--(int)-->(int2)--(l:Subset:`%1$s`) WHERE ID(n)<>ID(l) AND (int:Object OR int:SubObject) AND (int2:Object OR int2:SubObject) "
                    + "MERGE (n)-[:References]->(l)", applicationContext);
    neo4jAL.executeQuery(refreshLinkReq);
    String refreshConnections =
            String.format("MATCH (n:Subset:`%1$s`)-[]->(o) WHERE NOT (n)<-[]-(:ArchiModel) AND (o:Object OR o:SubObject) "
                    + "SET o.Subset = [ x in o.Subset WHERE NOT x=n.Name ] DETACH DELETE n", applicationContext);
    neo4jAL.executeQuery(refreshConnections);
  }

  /** Refresh Archi models in the application */
  private void refreshArchiModel() throws Neo4jQueryException {
    String reqRefreshCount =
        String.format(
            "MATCH (s:ArchiModel:`%s`) " +
            "WITH s " +
            "MATCH (s)-[:Contains]->(o:Subset) WITH s, SUM(o.Count) as tot SET s.Count=tot RETURN s as archi, tot as total;",
            applicationContext);
    Result res = neo4jAL.executeQuery(reqRefreshCount);
    if(res.hasNext()) {
      Map<String, Object> results = res.next();
      Long count = (Long) results.get("total");
      Node n = (Node) results.get("archi");

      if(count == 0L) n.delete();
    }


  }

  /**
   * Clean one specific tag from all the objects
   * @param tag
   * @throws Neo4jQueryException
   */
  private void cleanOneTag(String tag) throws Neo4jQueryException {
    String removeTagsQuery =
        String.format(
            "MATCH (o:`%1$s`) WHERE EXISTS(o.Tags) SET o.Tags = [ x IN o.Tags WHERE NOT x=$tag ] RETURN DISTINCT COLLECT(o.Tags) as removedTags;",
            applicationContext);
    Map<String, Object> params = Map.of("tag", tag);
    neo4jAL.executeQuery(removeTagsQuery, params);
    neo4jAL.logError(
        String.format(
            "Architecture Tag [ %s ] in application '%s' was not in a good format, and was removed from the database.",
                tag, applicationContext));
  }

  @Override
  public String getTagPrefix() {
    return getPrefix();
  }

  @Override
  public void setTagPrefix(String value) throws FileNotFoundException, MissingFileException {
    setPrefix(value);
  }

  @Override
  public void refresh() throws Neo4jQueryException {
    refreshSubset();
    refreshArchiModel();
  }

  @Override
  public Node group(String groupName, List<Node> nodeList) throws Neo4jQueryException {
    String[] cleanedGroupName = groupName.replace(getTagPrefix(), "").split("\\$");
    if (cleanedGroupName.length < 2) { // The group is not in a correct format
      cleanOneTag(groupName);
      return null;
    }

    String nameView = cleanedGroupName[0];
    String nameSubset = cleanedGroupName[1];

    // Create archi model
    String modelIdReq =
        String.format("MATCH (n:ArchiModel:`%s`) RETURN n.ModelId as modelId", applicationContext);
    Result res = this.neo4jAL.executeQuery(modelIdReq);
    String temp;
    Long maxId = 1L;
    while (res.hasNext()) {
      try {
        temp = (String) res.next().get("modelId");
        if (Long.parseLong(temp) >= maxId) {
          maxId = Long.parseLong(temp) + 1;
        }
      } catch (ClassCastException | NumberFormatException ignored) {
      }
    }

    // Merge & update
    String req =
        String.format(
            "MERGE (n:ArchiModel:`%s` { Type:'archimodel', Color:'rgb(34,199,214)',Name:$groupName} ) "
                + "SET n.Count=CASE WHEN EXISTS(n.Count) THEN n.Count + $count ELSE $count END SET n.ModelId=$maxModelID "
                + "RETURN n as node;",
            applicationContext);
    Map<String, Object> params =
        Map.of(
            "groupName",
            nameView,
            "count",
            new Long(nodeList.size()),
            "maxModelID",
            maxId.toString());
    Result result = neo4jAL.executeQuery(req, params);
    Node archiNode = (Node) result.next().get("node");

    // Create the subset
    String subsetIdReq =
        String.format("MATCH (n:Subset:`%s`) RETURN n.SubsetId as subsetId", applicationContext);
    Result resSubsetId = this.neo4jAL.executeQuery(subsetIdReq);
    String tempSub;
    Long maxIdSub = 1L;
    while (resSubsetId.hasNext()) {
      try {
        tempSub = (String) resSubsetId.next().get("subsetId");
        if (Long.parseLong(tempSub) >= maxIdSub) {
          maxIdSub = Long.parseLong(tempSub) + 1;
        }
      } catch (ClassCastException | NumberFormatException ignored) {
      }
    }

    // Merge the relationship between the Archi model and the subSet
    String reqSubset =
        String.format(
            "MERGE (n:Subset:`%s` { Type:'subset', Color:'rgb(34,199,214)',Name:$groupName } ) "
                + "SET n.ModelId= $maxModelID  "
                + "SET n.Count=CASE WHEN EXISTS(n.Count) THEN n.Count + $count ELSE $count END SET n.SubsetId=$subsetID "
                + "WITH n as node "
                + "MATCH (n:ArchiModel:`%1$s`) WHERE ID(n)=$idArchi MERGE (n)-[:Contains]->(node) "
                + "RETURN node as node;",
            applicationContext);
    Map<String, Object> paramsSubset =
        Map.of(
            "groupName",
            nameSubset,
            "count",
            new Long(nodeList.size()),
            "maxModelID",
            maxId.toString(),
            "subsetID",
            maxIdSub.toString(),
            "idArchi",
            new Long(archiNode.getId()));
    Result resSubset = neo4jAL.executeQuery(reqSubset, paramsSubset);

    Node subsetNode = (Node) resSubset.next().get("node");
    Long idSubset = new Long(subsetNode.getId());

    neo4jAL.logInfo("Subset node with ID :" + idSubset);

    // Add the objects and the SubObjects to the subset
    Map<String, Object> paramsNode;
    for (Node rObject : nodeList) {
      // Link objects
      paramsNode = Map.of("idObj", rObject.getId(), "idSubset", idSubset, "subsetName", nameSubset);

      String reObj =
          String.format(
              "MATCH (o:Object:`%s`) WHERE ID(o)=$idObj "
                  + "SET o.Subset = CASE WHEN o.Subset IS NULL THEN [$subsetName] ELSE o.Subset + $subsetName END "
                  + "WITH o as obj "
                  + "MATCH (newS:Subset) WHERE ID(newS)=$idSubset "
                  + "MERGE (newS)-[:Contains]->(obj) ",
              applicationContext);

      String subObj =
          String.format(
              "MATCH (o:Object:`%s`)<-[:BELONGTO]-(j:SubObject) WHERE ID(o)=$idObj "
                  + "SET j.Subset = CASE WHEN j.Subset IS NULL THEN [$subsetName] ELSE o.Subset + $subsetName END "
                  + "WITH j "
                  + "MATCH (newS:Subset) WHERE ID(newS)=$idSubset MERGE (newS)-[:Contains]->(j)  ",
              applicationContext);

      neo4jAL.executeQuery(reObj, paramsNode);
      neo4jAL.executeQuery(subObj, paramsNode);
    }

    return subsetNode;
  }
}
