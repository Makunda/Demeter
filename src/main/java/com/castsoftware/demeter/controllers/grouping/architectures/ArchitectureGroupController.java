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
import org.neo4j.graphdb.Transaction;

import java.util.*;

public class ArchitectureGroupController extends AGrouping {

  private final Set<String> createdArchitectures ;


  public ArchitectureGroupController(Neo4jAL neo4jAL, String applicationContext) {
    super(neo4jAL, applicationContext);
    createdArchitectures = new HashSet<>();
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
  }  public static String getPrefix() {
    return Configuration.getBestOfALl("demeter.prefix.architecture_group");
  }

  /**
   * Refresh the subsets in the application
   *
   * @throws Neo4jQueryException
   */
  public void refreshSubset(String architectureName) throws Neo4jQueryException {

    // Match all subset in the architecture
    String reqRefreshCount =
        String.format(
            "MATCH  (a:ArchiModel:`%1$s`)-[]->(s:Subset:`%1$s`) WHERE a.Name=$archiName " +
            "WITH s " +
            "MATCH (s)-[:Contains]->(o:Object) " +
            "WITH s, COUNT(o) as tot SET s.Count=tot RETURN s as subset, tot as total",

            applicationContext);
    Map<String, Object> parameters = Map.of("archiName", architectureName);
    Result res = neo4jAL.executeQuery(reqRefreshCount, parameters);

    if(res.hasNext()) {
      Map<String, Object> results = res.next();
      Long count = (Long) results.get("total");
      Node n = (Node) results.get("subset");

      if(count == 0L) {
        n.delete();
      }
    }

    // Generate random tag

    StringBuilder generatedString = new StringBuilder();
    Random r = new Random();
    String alphabet = "123456789azertyuiiopqsdfghjklmwxcvbnxyz";
    for (int i = 0; i < 10; i++) {
      generatedString.append(alphabet.charAt(r.nextInt(alphabet.length())));
    }

    neo4jAL.logInfo("Flagging objects and subObjects ...");
    // Get Subsets and objects, flag objects
    String req = String.format("MATCH (a:ArchiModel:`%1$s`)  WHERE a.Name=$archiName " +
                "WITH a MATCH (a)-[]->(s:Subset:`%1$s`)-[]->(o) WHERE (o:Object OR o:SubObject) " +
                "SET o.TempSubset_%2$s=s.Name SET o.Subset = [ x in o.Subset WHERE NOT x=s.Name ];", applicationContext, generatedString.toString());
    neo4jAL.executeQuery(req, parameters);

    neo4jAL.logInfo("Getting relationships ...");
    // Linking subset
    String req2 = String.format("MATCH (o:`%1$s`)-[]->(o2:`%1$s`) WHERE NOT o.TempSubset_%2$s=o2.TempSubset_%2$s " +
            "AND (o:Object OR o:SubObject) AND (o2:Object OR o2:SubObject) " +
          "RETURN DISTINCT o.TempSubset_%2$s as source_module, o2.TempSubset_%2$s as target_module", applicationContext, generatedString.toString());

    Result res2 = neo4jAL.executeQuery(req2, parameters);
    if(!res.hasNext()) neo4jAL.logInfo("No subset to relink.");

    while (res2.hasNext()) {
      Map<String, Object> t = res2.next();
      neo4jAL.logInfo(String.format("Create references between subset : '%s' and subset '%s'.",  t.get("source_module"), t.get("target_module")));

      String reqRel = String.format("MATCH (a:ArchiModel:`%1$s`)-[]->(s:Subset:`%1$s`), (a:ArchiModel:`%1$s`)-[]->(s2:Subset:`%1$s`) WHERE a.Name=$archiName " +
                    "AND s.Name=$subsetSource AND s2.Name=$subsetTarget " +
                    "MERGE (s)-[:References]->(s2)", applicationContext);
      Map<String, Object> params = Map.of("archiName", architectureName, "subsetSource",  t.get("source_module"), "subsetTarget", t.get("target_module"));
      neo4jAL.executeQuery(reqRel, params);
    }


    // Clean temp tags
    String removeTagReq = String.format("MATCH (o:`%1$s`) WHERE (o:Object OR o:SubObject) AND EXISTS(o.TempSubset_%2$s) REMOVE o.TempSubset_%2$s", applicationContext, generatedString.toString());
    neo4jAL.executeQuery(removeTagReq);


    /*String refreshConnections =
            String.format("MATCH (a:ArchiModel:`%1$s`)-[]->(n:Subset:`%1$s`)-[]->(o) WHERE a.Name=$archiName AND NOT (n)<-[]-(:ArchiModel) AND (o:Object OR o:SubObject) "
                    + "SET o.Subset = [ x in o.Subset WHERE NOT x=n.Name ] DETACH DELETE n", applicationContext);
    neo4jAL.executeQuery(refreshConnections, parameters);*/
    neo4jAL.logInfo("Subsets Connections were refreshed..");
  }  public static void setPrefix(String value) throws FileNotFoundException, MissingFileException {
    Configuration.setEverywhere("demeter.prefix.architecture_group", value);
  }

  /** Refresh Archi models in the application */
  public void refreshArchiModel(String architectureName) throws Neo4jQueryException {
    String reqRefreshCount =
        String.format(
            "MATCH (s:ArchiModel:`%s`) WHERE s.Name=$archiName  " +
            "WITH s " +
            "MATCH (s)-[:Contains]->(o:Subset) WITH s, SUM(o.Count) as tot SET s.Count=tot RETURN s as archi, tot as total;",
            applicationContext);
    Map<String, Object> parameters = Map.of("archiName", architectureName);
    Result res = neo4jAL.executeQuery(reqRefreshCount, parameters);

    if(res.hasNext()) {
      Map<String, Object> results = res.next();
      Long count = (Long) results.get("total");
      Node n = (Node) results.get("archi");

      if(count == 0L) n.delete();
    }
    neo4jAL.logInfo("Archi model was refreshed..");


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

  /**
   * Refresh the connection of nodes present in the subset
   * @param nodeList
   * @param idSubset
   * @param nameSubset
   * @throws Neo4jQueryException
   */
  protected void refreshNodes(List<Node> nodeList, Long idSubset, String nameSubset) throws Neo4jQueryException {
    Map<String, Object> paramsNode;
    Long count = 0L;
    String reObj;
    String subObj;

    // This function needs to be optimized as much as possible
    for (Node rObject : nodeList) {
      // Link objects
      try(Transaction tn = neo4jAL.getTransaction()) {
        count++;
        if(count % 100 == 0) neo4jAL.logInfo("Still refreshing nodes ( "+count+" on " + nodeList.size() + " ) ");

        paramsNode = Map.of("idObj", rObject.getId(), "idSubset", idSubset, "subsetName", nameSubset);

        reObj = "MATCH (newS:Subset) WHERE ID(newS)=$idSubset " +
                "WITH newS " +
                "MATCH (o:Object:`"+applicationContext+"`) WHERE ID(o)=$idObj "
                + "SET o.Subset = CASE WHEN o.Subset IS NULL THEN [$subsetName] ELSE o.Subset + $subsetName END "
                + "WITH newS, o as obj "
                + "MERGE (newS)-[:Contains]->(obj) ";

        subObj = "MATCH (newS:Subset) WHERE ID(newS)=$idSubset " +
                "WITH newS " +
                "MATCH (o:Object:`"+applicationContext+"`)<-[:BELONGTO]-(j:SubObject) WHERE ID(o)=$idObj "
                + "SET j.Subset = CASE WHEN j.Subset IS NULL THEN [$subsetName] ELSE o.Subset + $subsetName END "
                + "WITH newS, j "
                + "MERGE (newS)-[:Contains]->(j)  ";

        tn.execute(reObj, paramsNode);
        tn.execute(subObj, paramsNode);

        tn.commit();
      } catch (Exception e) {
        neo4jAL.logError("Failed to refresh node with id "+rObject.getId(), e);
      }

    }

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
    for(String archiName : createdArchitectures) {
      refreshSubset(archiName);
      refreshArchiModel(archiName);
    }
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
    neo4jAL.logInfo(String.format("DEBUG :: Group Name creating : %s -> %s", nameView, nameSubset));

    createdArchitectures.add(nameView);

    // Create archi model
    String modelIdReq =
        String.format("MATCH (n:ArchiModel:`%s`) RETURN n.ModelId as modelId", applicationContext);
    Result res = this.neo4jAL.executeQuery(modelIdReq);
    String temp;
    long maxId = 1L;
    while (res.hasNext()) {
      try {
        temp = (String) res.next().get("modelId");
        if (Long.parseLong(temp) >= maxId) {
          maxId = Long.parseLong(temp) + 1;
        }
      } catch (ClassCastException | NumberFormatException ignored) {
      }
    }

    // Merge & update Archi model
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
                Long.toString(maxId));
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

    // Subset parameters
    Map<String, Object> paramsSubset =
            Map.of(
                    "groupName",
                    nameSubset,
                    "count",
                    new Long(nodeList.size()),
                    "maxModelID",
                    Long.toString(maxId),
                    "subsetID",
                    maxIdSub.toString(),
                    "idArchi",
                    new Long(archiNode.getId()));


    // Verify if the subset already exists
    String reqSubsetExists = String.format("MATCH (archi:ArchiModel:`%1$s`)-[:Contains]->(n:Subset:`%s`) WHERE ID(archi)=$idArchi AND n.Name=$groupName " +
                "RETURN n as sub", applicationContext);
    Result resSubsetExist = neo4jAL.executeQuery(reqSubsetExists, paramsSubset);

    Node subsetNode = null;
    if(resSubsetExist.hasNext()) {
      // Subset already exists Update
      neo4jAL.logInfo(String.format("Subset '%s' already exists for subset with id: %d.", nameSubset, maxId));
      subsetNode = (Node) resSubsetExist.next().get("sub");
      subsetNode.setProperty("ModelId", Long.toString(maxId));
      subsetNode.setProperty("SubsetId",maxIdSub.toString());

      Long count = subsetNode.hasProperty("Count") ? (Long) subsetNode.getProperty("Count") : 0L;
      subsetNode.setProperty("Count", count + nodeList.size());

    } else {
      // Merge the relationship between the Archi model and the subSet
      String reqSubset =
              String.format(
                      "CREATE (n:Subset:`%s` { Type:'subset', Color:'rgb(34,199,214)',Name:$groupName } ) "
                              + "SET n.ModelId= $maxModelID "
                              + "SET n.Count=CASE WHEN EXISTS(n.Count) THEN n.Count + $count ELSE $count END SET n.SubsetId=$subsetID "
                              + "WITH n as node "
                              + "MATCH (archi:ArchiModel:`%1$s`) WHERE ID(archi)=$idArchi MERGE (archi)-[:Contains]->(node) " +
                              "RETURN node as node",
                      applicationContext);

      Result resSubset = neo4jAL.executeQuery(reqSubset, paramsSubset);

      subsetNode = (Node) resSubset.next().get("node");
    }

    Long idSubset = new Long(subsetNode.getId());
    neo4jAL.logInfo("Subset node with ID :" + idSubset);

    // Add the objects and the SubObjects to the subset
    refreshNodes(nodeList, idSubset, nameSubset);

    return subsetNode;
  }
}
