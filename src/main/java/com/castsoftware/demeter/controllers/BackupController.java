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

package com.castsoftware.demeter.controllers;

import com.castsoftware.demeter.config.Configuration;
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.BackupNode;
import com.castsoftware.demeter.models.Level5Node;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.List;

public class BackupController {

    private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String IMAGING_LEVEL_5_LABEL = Configuration.get("imaging.node.level5.label");
    private static final String IMAGING_AGGREGATES = Configuration.get("imaging.node.level_nodes.links");
    private static final String GENERATED_LEVEL_PREFIX = Configuration.get("demeter.prefix.generated_level_prefix");

    public static List<Node> undoGroup(Neo4jAL neo4jAL, String applicationContext) throws Neo4jBadRequestException, Neo4jQueryException, Neo4jNoResult, Neo4jBadNodeFormatException {
        List<Node> returnNodes = new ArrayList<>();

        String forgedRetrieveLeve5 = String.format("MATCH (o:%s:%s) RETURN o as node", IMAGING_LEVEL_5_LABEL, applicationContext);
        Result result = neo4jAL.executeQuery(forgedRetrieveLeve5);

        List<Level5Node> toCheckLevels = new ArrayList<>();
        List<String> deletedLevelName = new ArrayList<>();

        // Delete Demeter level5 group based on their full name
        while(result.hasNext()) {
            Node n = (Node) result.next().get("node");
            Level5Node level = Level5Node.fromNode(neo4jAL, n);

            String fullName = level.getFullName();

            // Get last identifier, corresponding to the level 5
            neo4jAL.logInfo("FullName detected " + fullName);

            if(fullName.matches("(.*)##" + GENERATED_LEVEL_PREFIX + "(.*)")) {
                // Is a demeter level
                neo4jAL.logInfo("Node is Demeter Generated. Will be deleted.");

                // Keep level to reassign nodes later
                deletedLevelName.add(level.getName());

                level.deleteNode();
            } else {
                // Is not a demeter level
                toCheckLevels.add(level);
            }
        }

        // Get all  backup node
        List<BackupNode> backupNodeList = BackupNode.getAllNodes(neo4jAL);

        // Recreate level 5
        for(BackupNode bkn : backupNodeList) {
            Node n = bkn.startBackup();
            returnNodes.add(n);
        }

        // Relink objects to their parent levels for each deleted level
        String forgedLabel = null;
        String forgeRequest = null;
        for(String levelName : deletedLevelName ) {

            neo4jAL.logInfo("Reassign tags for old level with name : " + levelName);
            forgedLabel = IMAGING_OBJECT_LABEL + ":" + applicationContext;

            // Find nodes with the ancient level 5 name
            forgeRequest = String.format("MATCH (o:%1$s)<-[:%2$s]-(l:%3$s) WHERE o.Level='%4$s' AND NOT l.Name='%4$s' SET o.Level=l.Name",
                    forgedLabel, IMAGING_AGGREGATES, IMAGING_LEVEL_5_LABEL, levelName);
            neo4jAL.executeQuery(forgeRequest);
        }

        // Recount level
        for(Level5Node level : toCheckLevels) {
            GroupController.refreshLevelLinks(neo4jAL, level.getNode());
            GroupController.refreshLevelCount(neo4jAL, applicationContext, level.getNode());
        }

        return  returnNodes;
    }
}
