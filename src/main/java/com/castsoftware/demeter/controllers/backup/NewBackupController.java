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

package com.castsoftware.demeter.controllers.backup;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.controllers.grouping.levels.AdvancedLevelGrouping;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.stream.Collectors;

public class NewBackupController {

  private final Neo4jAL neo4jAL;
  private final String application;
  private final String savePrefix;




  /**
   * Constructor
   *
   * @param neo4jAL Neo4j Access Layer
   * @param application Name of the application
   */
  public NewBackupController(Neo4jAL neo4jAL, String application) {
    this.neo4jAL = neo4jAL;
    this.application = application;
    this.savePrefix = Configuration.get("backup.node.property_prefix");
  }

  /**
   * Export a Save
   *
   * @param name Name of the save to export
   * @param location Location to export the save file
   */
  public void exportState(String name, String location) {
    // TODO
  }

  public void importSave(Neo4jAL neo4jAL, String location) {
    // TODO
  }

  /**
   * Rollack to a previous state in the application
   *
   * @param name Name of the save
   */
  public void rollBackToSave(String name) throws Exception {
    Map<String, List<Long>> levelMap = new HashMap<>(); // Init Level map
    String saveProperty = getSaveProperty(name); // Declare property

    AdvancedLevelGrouping advancedG = new AdvancedLevelGrouping(this.neo4jAL);

    // Get groups of nodes to reassign
    levelMap = MasterSaveNode.getDifferences(this.neo4jAL, this.application, saveProperty);

    int count = 0; // Count
    String taxonomy;
    String[] splitTax;

    // Parse entry in the map
    for (Map.Entry<String, List<Long>> record : levelMap.entrySet()) {
      try {
        taxonomy = record.getKey();
        splitTax = taxonomy.split("##");
        advancedG.groupWithTaxonomy(
            this.application,
            splitTax[0],
            splitTax[1],
            splitTax[2],
            splitTax[3],
            splitTax[4],
            record.getValue());

        // Add count to list
        count += record.getValue().size();

      } catch (Neo4jQueryException | Neo4jNoResult e) {
        // Get example of nodes that throw the error
        String nodesExample =
            record.getValue().stream()
                .limit(5)
                .map(Objects::toString)
                .collect(Collectors.joining(", "));

        // Log error
        neo4jAL.logError(
            String.format(
                "Failed to reassign nodes to their category."
                    + "Taxonomy detected [%s], Example of nodes nodes : [%s].",
                record.getKey(), nodesExample));

        // Continue
      }
    }

    // end of the process
    neo4jAL.logInfo(String.format("%d nodes have been saved during this procedure.", count));
  }

  /**
   * Format correctly the save property to be applied / retreived from a node
   *
   * @param name Name of the save
   * @return The formatted save prop
   */
  private String getSaveProperty(String name) {
    return String.format("%s%s", this.savePrefix, name);
  }

  /**
   * Get the list of all saves in the application
   *
   * @return The list of save detected
   */
  public List<String> getListSave() throws Exception {
    try {

      // Return the distinct list
      return MasterSaveNode.getListMasterSave(neo4jAL, application);
    } catch (Exception e) {
      // Failed to execute the original query
      neo4jAL.logError(
          String.format("Failed to load the list of Save in the application '%s'.", application));
      throw new Exception("Failed to get the list of save.");
    }
  }

  /**
   * Get the list of all saves in the application
   *
   * @param name Name of the save to remove
   */
  public void deleteSave(String name) throws Exception {
    try {
      // Delete save node
      MasterSaveNode.deleteMasterSave(neo4jAL, application, name);
      neo4jAL.logInfo(
          String.format(
              "The save '%s' has been removed from application '%s'.", name, application));
    } catch (Exception e) {
      neo4jAL.logError(
          String.format(
              "Failed to remove the save with name '%s' in the application '%s'.",
              name, application));
      throw new Exception("Failed to remove the save in the application.");
    }
  }

  /**
   * Save application state
   *
   * @param name Name of the save
   */
  public void saveState(String name) throws Exception, Neo4jQueryException {
    Map<Long, String> levelMap = new HashMap<>(); // Init Level map

    // Get the taxonomy map in the application
    try {
      levelMap = BackupUtils.getLevel5Taxonomy(this.neo4jAL, this.application);
    } catch (Neo4jQueryException e) {
      neo4jAL.logError(
          String.format("Failed to save the state of the application '%s'.", this.application));
      throw new Exception("Failed to save application's state. Check the logs");
    }

    // Log backup
    neo4jAL.logInfo(String.format("%d levels will be backup for save with name '%s'.", levelMap.size(), name));

    // Apply on each node a save property with its taxonomy
    String match =
        String.format(
            "MATCH (l:Level5:`%1$s`)-[:Aggregates]->(o:Object:`%1$s`) WHERE ID(l)=$idLevel "
                + "RETURN DISTINCT ID(o) as nodeId;",
            this.application);

    // Loop parameters
    int count = 0;

    // For all level / object in the application apply a property with the taxonomy
    Result res;
    String taxonomy;
    List<Long> nodeIdList;
    for (Map.Entry<Long, String> en : levelMap.entrySet()) { // Iterate over the taxonomies
      // Init variables
      nodeIdList = new ArrayList<>();
      taxonomy = en.getValue();

      try {
        // Get the list of nodes to flag for each level 5
        res = neo4jAL.executeQuery(match, Map.of("idLevel", en.getKey()));
        while (res.hasNext()) {
          nodeIdList.add((Long) res.next().get("nodeId")); // Get node and set property
          count++;
        }

        // Save the node list
        MasterSaveNode.saveObjects(neo4jAL, application, name, taxonomy, nodeIdList);
      } catch (Neo4jQueryException err) {
        neo4jAL.logError(
            String.format(
                "Failed to retrieve the list of node under level '%s' ( id : [%d] ).",
                this.application, en.getKey()));
      }
    }

    // end of the process
    neo4jAL.logInfo(String.format("%d nodes have been saved during this procedure.", count));
  }
}
