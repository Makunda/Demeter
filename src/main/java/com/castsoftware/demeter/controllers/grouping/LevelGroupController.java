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
import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.imaging.Level5Node;
import com.castsoftware.demeter.models.imaging.ModuleNode;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class LevelGroupController {

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

    public static void refreshLevelLinks(Neo4jAL neo4jAL, Node nodeLevel) throws Neo4jQueryException {
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

    /**
     * Refresh the count parameter for a level ( Counting objects linked to it )
     * @param neo4jAL Neo4j access layer
     * @param applicationContext Application concerned by the change
     * @param levelNode Level node necessitating a
     * @return
     * @throws Neo4jQueryException
     */
    public static Node refreshLevelCount(Neo4jAL neo4jAL, String applicationContext, Node levelNode) throws Neo4jQueryException {
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
        }

        return levelNode;
    }

    /**
     * Get the level 5 present in the node list. Level are returned as a map, with the key corresponding to the level node and the value their frequency.
     * The return map is sorted by ascending order.
     * @param neo4jAL Neo4j Access Layer
     * @param nodeList List of the node used to extract level5
     * @return The map containing level5 nodes and their usage frequency as a Stream
     * @throws Neo4jNoResult If no Level 5 were detected
     */
    private static Iterator<Map.Entry<Node, Integer>> getLevel5(Neo4jAL neo4jAL, List<Node> nodeList) throws Neo4jNoResult {

        RelationshipType relLevel = RelationshipType.withName(IMAGING_AGGREGATES);

        // Get Actual Level 5 and connections
        Map<Node, Integer> level5map = new HashMap<>();

        for( Node rObject : nodeList ) {

            Iterator<Relationship> oldRel = rObject.getRelationships(Direction.INCOMING, relLevel).iterator();
            neo4jAL.logInfo("Try to find level 5 node in node with id : " + rObject.getId());
            if (oldRel.hasNext()) {
                Node level5 = oldRel.next().getStartNode();

                level5map.putIfAbsent(level5, 0);
                level5map.compute(level5, (x, v) -> v + 1);
            }
        }

        if(level5map.size() == 0) {
            throw new Neo4jNoResult("Cannot find a valid Level 5 for the tag", "No relation detected between tagged node and Level5" , ERROR_PREFIX+"GROS1");
        }

        return level5map.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).iterator();
    }

    public static Node groupSingleTag(Neo4jAL neo4jAL, String applicationContext, String groupName, List<Node> nodeList) throws Neo4jNoResult, Neo4jQueryException, Neo4jBadNodeFormatException, Neo4jBadRequestException {
        RelationshipType aggregatesRel = RelationshipType.withName(IMAGING_AGGREGATES);

        neo4jAL.logInfo("### Size of nodeList : " + nodeList.size() );

        Node oldLevel5Node = null;

        // Retrieve most encountered Level 5
        for (Iterator<Map.Entry<Node, Integer>> it = getLevel5(neo4jAL, nodeList); it.hasNext(); ) {
            Map.Entry<Node, Integer> level5entry = it.next();

            // Get first node. Corresponding to the most frequent one
            if(oldLevel5Node == null) {
                oldLevel5Node = level5entry.getKey();
            }

            // Save the level and their nodes
            Level5Node level = Level5Node.fromNode(neo4jAL, level5entry.getKey());
            level.createLevel5Backup(applicationContext, nodeList);
            neo4jAL.logInfo("Backed up Level5 node with name " + level.getName() + " used by " + level5entry.getValue() + " objects");
        }


        neo4jAL.logInfo("Level 5 found, with Id : " + oldLevel5Node.getId());


        // Get associated Level 4 full name
        // You cannot get it directly by looking at the relationships because sometimes there are several Level4 linked to the same node.
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

        // Merge new Level 5 and labelled them with application's name
        String forgedLabel = applicationContext + ":" +  Level5Node.getLabel();
        String forgedFindLevel = String.format("MATCH (o:%1$s) WHERE o.%2$s='%3$s' RETURN o as node;", forgedLabel, Level5Node.getNameProperty() , forgedName);

        Node newLevelNode = null;
        Result result = neo4jAL.executeQuery(forgedFindLevel);
        if(result.hasNext()) {
            // Module with same name was found, and results will be merge into it
            newLevelNode = (Node) result.next().get("node");
        } else {
            // Create a new module
            Label applicationLabel = Label.label(applicationContext);
            // Forge properties
            String levelName = forgedName;
            Boolean concept = Boolean.FALSE;
            Boolean drillDown = Boolean.TRUE;
            String fullName = level4FullName + "##" + GENERATED_LEVEL_IDENTIFIER + levelName;
            String color = (String) oldLevel5Node.getProperty(Level5Node.getColorProperty());
            Long level = 5L;
            Long count = ((Integer) nodeList.size()).longValue();
            String shade = (String) oldLevel5Node.getProperty(Level5Node.getShadeProperty());

            Level5Node newLevel = new Level5Node(neo4jAL, levelName, concept, drillDown, fullName, color, level, count, shade);
            newLevelNode = newLevel.createNode();
            newLevelNode.addLabel(applicationLabel);

        }


        // Link new level to Level 4
        level4Node.createRelationshipTo(newLevelNode, aggregatesRel);

        //Delete old relationships, to not interfere with the new level
        for (Node n : nodeList) {
            // Find and Delete Old Relationships
            for( Relationship relN : n.getRelationships(Direction.INCOMING, aggregatesRel)) {
                if(relN.getStartNodeId() == oldLevel5Node.getId()) {
                    relN.delete();
                }
            }

            // Relink to new level
            Relationship r = newLevelNode.createRelationshipTo(n, aggregatesRel);
            // Change the level name to the new one of each node
            n.setProperty(IMAGING_OBJECT_LEVEL, forgedName);
        }

        // Recompute for New and old
        refreshLevelLinks(neo4jAL, newLevelNode);

        // Refresh old Level 5
        refreshLevelCount(neo4jAL, applicationContext, oldLevel5Node);
        refreshLevelLinks(neo4jAL, oldLevel5Node);

        return newLevelNode;
    }

    public static List<Node> groupAllLevels(Neo4jAL neo4jAL, String applicationContext) throws Neo4jQueryException {
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


}
