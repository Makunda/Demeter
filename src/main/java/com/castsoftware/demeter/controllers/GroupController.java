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
import com.castsoftware.demeter.models.Level5Node;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class GroupController {

    // Imaging Conf
    private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String IMAGING_OBJECT_TAGS = Configuration.get("imaging.link.object_property.tags");
    private static final String IMAGING_OBJECT_LEVEL = Configuration.get("imaging.node.object.level");
    private static final String IMAGING_AGGREGATES = Configuration.get("imaging.node.level_nodes.links");
    private static final String IMAGING_LEVEL_REFERENCES = Configuration.get("imaging.node.level_nodes.references");
    private static final String IMAGING_LEVEL4_LABEL = Configuration.get("imaging.node.level4.label");

    // Demeter Conf
    private static final String GROUP_TAG_IDENTIFIER = Configuration.get("demeter.prefix.community_group");
    private static final String AUTO_GROUP_TAG_IDENTIFIER = Configuration.get("demeter.prefix.auto_community_group");
    private static final String GENERATED_LEVEL_IDENTIFIER = Configuration.get("demeter.prefix.generated_level_prefix");

    // Class Conf
    private static final String ERROR_PREFIX = "GROCx";

    private static void refreshLevelLinks(Neo4jAL neo4jAL, Node nodeLevel) throws Neo4jQueryException {
        Label objectLabel = Label.label(IMAGING_OBJECT_LABEL);
        RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);
        RelationshipType referenceRel = RelationshipType.withName(IMAGING_LEVEL_REFERENCES);


        // Link the objects to the New Level 5
        Set<Node> toOtherLevel5 = new HashSet<>();
        Set<Node> fromOtherLevel5 = new HashSet<>();

        //Remove actual level relationships
        for(Relationship rel : nodeLevel.getRelationships(referenceRel)) {
            rel.delete();
        }

        //Get node list
        for (Relationship rel: nodeLevel.getRelationships(Direction.OUTGOING, aggregatesRel)) {

            // Get objects
            Node n = rel.getEndNode();
            if(!n.hasLabel(objectLabel)) continue;


            // List outgoing level 5
            String forgedToOtherLevel5 = String.format("MATCH (n)-->(:%1$s)<--(l:%2$s) WHERE ID(n)=%3$s RETURN l as level",
                    IMAGING_OBJECT_LABEL, Level5Node.getLabel(), n.getId() );

            // List incoming level 5
            String forgedFromOtherLevel5 = String.format("MATCH (n)<--(:%1$s)<--(l:%2$s) WHERE ID(n)=%3$s RETURN l as level",
                    IMAGING_OBJECT_LABEL, Level5Node.getLabel(), n.getId() );

            Result resTo = neo4jAL.executeQuery(forgedToOtherLevel5);
            while (resTo.hasNext()) {
                Node resToNode = (Node) resTo.next().get("level");
                Long id = resToNode.getId();

                if( id == nodeLevel.getId()) continue;
                toOtherLevel5.add(resToNode);
            }

            Result resFrom = neo4jAL.executeQuery(forgedFromOtherLevel5);
            while (resFrom.hasNext()) {
                Node resToNode = (Node) resFrom.next().get("level");
                Long id = resToNode.getId();

                if( id == nodeLevel.getId()) continue;
                fromOtherLevel5.add(resToNode);
            }


        }

        RelationshipType relReferences = RelationshipType.withName(IMAGING_LEVEL_REFERENCES);

        // Link to other level 5
        for (Node toLink : toOtherLevel5) {
            nodeLevel.createRelationshipTo(toLink, referenceRel);
        }

        // Link level 5 from other level 5
        for (Node fromLink : fromOtherLevel5) {
            fromLink.createRelationshipTo(nodeLevel, referenceRel);
        }

        // Display info
        List<String> toLevelName =  toOtherLevel5.stream().map(x -> (String) x.getProperty("Name")).collect(Collectors.toList());
        List<String> fromLevelName =  fromOtherLevel5.stream().map(x -> (String) x.getProperty("Name")).collect(Collectors.toList());
        neo4jAL.logInfo(toOtherLevel5.size() + " outgoing relationships were detected and created. To : " + String.join(",", toLevelName));
        neo4jAL.logInfo(fromOtherLevel5.size() + " incoming relationships were detected and created." + String.join(",", fromLevelName));


    }

    private static Node refreshLevelCount(Neo4jAL neo4jAL, String applicationContext, Node levelNode) throws Neo4jQueryException {
        // Update the old Level 5 and remove it is there no node linked to it
        String forgedNumConnected = String.format("MATCH (n:%1$s:%2$s)-[:%3$s]->(o:%4$s) WHERE ID(n)=%5$s RETURN COUNT(o) as countNode;",
                applicationContext, Level5Node.getLabel(), IMAGING_AGGREGATES, IMAGING_OBJECT_LABEL, levelNode.getId());

        Result resNumConnected = neo4jAL.executeQuery(forgedNumConnected);

        Long numLeft = 0L;
        if(resNumConnected.hasNext()) {
            numLeft = (Long) resNumConnected.next().get("countNode");
        }

        // Delete the oldLevel node if it's empty
        if (numLeft == 0) {
            // Detach
            for(Relationship rel : levelNode.getRelationships()) {
                rel.delete();
            }
            // Delete
            levelNode.delete();
            neo4jAL.logInfo("Level had no more relationships with objects and was deleted.");
        } else {
            // Update count property
            levelNode.setProperty(Level5Node.getCountProperty(), numLeft);
            neo4jAL.logInfo("Level still has " + numLeft + " relationships with objects and will not be deleted.");

            // Reconnect Old level to modules


        }

        return levelNode;
    }

    private static Node getLevel5(Neo4jAL neo4jAL, List<Node> nodeList) throws Neo4jNoResult {

        RelationshipType relLevel = RelationshipType.withName(IMAGING_AGGREGATES);

        // Get Actual Level 5 and connections
        int it = 0;
        Node oldLevel5Node = null;

        do {
            Node rObject = nodeList.get(it);
            Iterator<Relationship> oldRel = rObject.getRelationships(Direction.INCOMING, relLevel).iterator();
            neo4jAL.logInfo("Try to find level 5 node in node with id : " + rObject.getId());
            if (oldRel.hasNext()) {
                oldLevel5Node = oldRel.next().getStartNode();
            }

            it ++;
        } while (oldLevel5Node == null && it < nodeList.size() - 1);

        if(oldLevel5Node == null) {
            throw new Neo4jNoResult("Cannot find a valid Level 5 for the tag", "No relation detected between tagged node and Level5" , ERROR_PREFIX+"GROS1");
        }

        return oldLevel5Node;
    }

    public static Node groupSingleTag(Neo4jAL neo4jAL, String applicationContext, String groupName, List<Node> nodeList) throws Neo4jNoResult, Neo4jQueryException, Neo4jBadNodeFormatException, Neo4jBadRequestException {
        RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);

        neo4jAL.logInfo("### Size of nodeList : " + nodeList.size() );

        // Retrieve the First level 5 encountered
        Node oldLevel5Node = getLevel5(neo4jAL, nodeList);

        neo4jAL.logInfo("Level 5 found, with Id : " + oldLevel5Node.getId());


        Level5Node level5 = Level5Node.fromNode(neo4jAL, oldLevel5Node);
        level5.createLevel5Backup(applicationContext);

        // Update the old Level 5 and remove it is there no node linked to it
        String preReq = String.format("MATCH (n:%1$s:%2$s)-[:%3$s]->(o:%4$s) WHERE ID(n)=%5$s RETURN COUNT(o) as countNode;",
                applicationContext, Level5Node.getLabel(), IMAGING_AGGREGATES, IMAGING_OBJECT_LABEL, oldLevel5Node.getId());

        Result resConnected = neo4jAL.executeQuery(preReq);

        // Debug purposes, check the size of the level before any operation
        Long numIn = 0L;
        if(resConnected.hasNext()) {
            numIn = (Long) resConnected.next().get("countNode");
        }
        neo4jAL.logInfo("### Number of  object present in the parent level : " + numIn);


        // Get associated Level 4 full name
        // You cannot get it directly by looking at the relationships because there are several Level 4s linked to the same node.

        // remove ## for this level
        String oldFullName = (String) oldLevel5Node.getProperty(Level5Node.getFullNameProperty());
        String[] splitArr = oldFullName.split("##");
        String level4FullName = String.join("##", Arrays.copyOf(splitArr, splitArr.length - 1));

        String forgedLevel4 = String.format("MATCH (o:%1$s:%2$s) WHERE o.%3$s CONTAINS '%4$s' RETURN o as node;",
                IMAGING_LEVEL4_LABEL, applicationContext, Level5Node.getFullNameProperty(), level4FullName);

        Result resLevel4 = neo4jAL.executeQuery(forgedLevel4);

        if (!resLevel4.hasNext()) {
            Neo4jNoResult err = new Neo4jNoResult("Cannot find Level 4 node. Aborting grouping operation for tag :" + groupName, forgedLevel4, ERROR_PREFIX + "GROT1");
            neo4jAL.logError("Cannot find level4.", err);
            throw err;
        }

        Node level4Node = (Node) resLevel4.next().get("node");

        // Forge the name of the level by removing the tag identifier
        String forgedName = groupName.replace(GROUP_TAG_IDENTIFIER, "");

        // Create new Level 5 and labelled them with application's name
        Label applicationLabel = Label.label(applicationContext);
        // Forge properties
        String levelName = forgedName;
        Boolean concept = Boolean.FALSE;
        Boolean drillDown = Boolean.TRUE;
        String fullName = level4FullName + "##" + levelName;
        String color = (String) oldLevel5Node.getProperty(Level5Node.getColorProperty());
        Long level = 5L;
        Long count = ((Integer) nodeList.size()).longValue();
        String shade = (String) oldLevel5Node.getProperty(Level5Node.getShadeProperty());

        Level5Node newLevel = new Level5Node(neo4jAL, levelName, concept, drillDown, fullName, color, level, count, shade);
        Node newLevelNode = newLevel.createNode();
        newLevelNode.addLabel(applicationLabel);

        neo4jAL.logInfo("New Level node id :" + newLevelNode.getId());

        // Link new level to Level 4
        level4Node.createRelationshipTo(newLevelNode, aggregatesRel);

        //Delete old relationships, to not interfere with the new level
        for (Node n : nodeList) {
            // Find and Delete Old Relationship
            for( Relationship relN : n.getRelationships(Direction.INCOMING, aggregatesRel)) {
                if(relN.getStartNodeId() == oldLevel5Node.getId()) {
                    relN.delete();
                }
            }
        }

        String createdRel = "To nodes with id : ";
        for (Node n : nodeList) {
            // Relink to new level
            Relationship r = newLevelNode.createRelationshipTo(n, aggregatesRel);
            // Change the level name to the new one of each node
            n.setProperty(IMAGING_OBJECT_LEVEL, forgedName);

            createdRel += " to "+n.getId()+" rel with id : " + r.getId() +  " , ";
        }
        neo4jAL.logInfo(createdRel);

        // Recompute for New and old
        refreshLevelLinks(neo4jAL, newLevelNode);

        // Refresh old Level 5
        refreshLevelCount(neo4jAL, applicationContext, oldLevel5Node);
        refreshLevelLinks(neo4jAL, oldLevel5Node);

        return  newLevelNode;
    }

    public static List<Node> groupTags(Neo4jAL neo4jAL, String applicationContext) throws Neo4jQueryException {
        Map<String, List<Node>> groupMap = new HashMap<>();

        neo4jAL.logInfo("Starting Demeter level 5 grouping...");

        // Get the list of nodes prefixed by dm_tag
        String forgedTagRequest = String.format("MATCH (o:%1$s:%2$s) WHERE any( x in o.%3$s WHERE x CONTAINS '%4$s')  " +
                "WITH o, [x in o.%3$s WHERE x CONTAINS '%4$s'][0] as g " +
                "RETURN o as node, g as group;", IMAGING_OBJECT_LABEL, applicationContext, IMAGING_OBJECT_TAGS, GROUP_TAG_IDENTIFIER);

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

        neo4jAL.logInfo(groupMap.size() + " groups were identified.");

        List<Node> resNodes = new ArrayList<>();

        // Build a level 5 and attach the node list
        for (Map.Entry<String, List<Node>> entry : groupMap.entrySet()) {
            String groupName = entry.getKey();
            List<Node> nodeList = entry.getValue();

            if (nodeList.isEmpty()) continue;

            try {
                neo4jAL.logInfo("# Now processing group with name : " +groupName);
                Node n = groupSingleTag(neo4jAL, applicationContext, groupName, nodeList);
                resNodes.add(n);
            } catch (Exception | Neo4jNoResult | Neo4jBadNodeFormatException | Neo4jBadRequestException err) {
                 neo4jAL.logError("An error occurred trying to create Level 5 for nodes with tags : " + groupName, err);
            }
        }

        neo4jAL.logInfo("Demeter level 5 grouping finished.");
        neo4jAL.logInfo("Cleaning tags...");

        // Once the operation is done, remove Demeter tag prefix tags
        String removeTagsQuery = String.format("MATCH (o:%1$s) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS '%3$s' ] RETURN COUNT(o) as removedTags;",
                applicationContext, IMAGING_OBJECT_TAGS, GROUP_TAG_IDENTIFIER);
        Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery);

        if(tagRemoveRes.hasNext()) {
            Long nDel = (Long) tagRemoveRes.next().get("removedTags");
            neo4jAL.logInfo( "# " + nDel + " demeter 'group tags' were removed from the database.");
        }

        neo4jAL.logInfo("Cleaning Done !");

        return resNodes;
    }

    public List<Node> rollback() {
        return null;
    }
}
