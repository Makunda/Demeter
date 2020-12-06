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
import com.castsoftware.demeter.models.imaging.ModuleNode;
import org.neo4j.graphdb.*;
import org.w3c.dom.traversal.NodeFilter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleGroupController {

    // Static Imaging nodes
    private static final String IMAGING_SUB_OBJECT_LABEL = Configuration.get("imaging.node.sub_object.label");
    private static final String IMAGING_OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String IMAGING_RAW_LABEL = Configuration.get("imaging.node.raw.label");
    private static final String IMAGING_MODULE_LABEL = Configuration.get("imaging.node.module.label");
    private static final String IMAGING_MODULE_NAME = Configuration.get("imaging.node.module.name");
    private static final String IMAGING_MODULE_AIP_ID = Configuration.get("imaging.node.module.aipId");
    private static final String IMAGING_OBJECT_TAGS = Configuration.get("imaging.link.object_property.tags");

    private static final String MODULE_PROPERTY = Configuration.get("imaging.node.module_property");
    private static final String MODULE_SUBSET_PROPERTY = Configuration.get("imaging.node.sub_object.subset_property");

    // Demeter Conf
    private static final String GROUP_MODULE_TAG_IDENTIFIER = Configuration.get("demeter.prefix.module_group");
    private static final String GENERATED_MODULE_IDENTIFIER = Configuration.get("demeter.prefix.generated_module_prefix");


    // Static Imaging relationships
    private static final String IMAGING_CONTAINS = Configuration.get("imaging.node.module.links.to_objects");
    private static final String IMAGING_REFERENCES = Configuration.get("imaging.node.module.links.to_modules");
    private static final String IMAGING_BELONG_TO = Configuration.get("imaging.node.sub_object.link.to_objects");


    public static void refreshModuleLinks(Neo4jAL neo4jAL, Node nodeModule) throws Neo4jQueryException {
        RelationshipType containsRel = RelationshipType.withName(IMAGING_CONTAINS);
        RelationshipType referencesRel = RelationshipType.withName(IMAGING_REFERENCES);

        // Link the objects to the New Level 5
        Set<Node> toOtherModule = new HashSet<>();
        Set<Node> fromOtherModule = new HashSet<>();

        //Get node list
        for (Relationship rel: nodeModule.getRelationships(Direction.OUTGOING, containsRel)) {

            // Get objects and assert they are objects
            Node n = rel.getEndNode();
            // Assert the node has a module parameter
            if(!n.hasProperty(MODULE_PROPERTY)) continue;

            // List outgoing modules
            String forgedToOtherModules = String.format("MATCH (n)-->()<-[:%1$s]-(l:%2$s) WHERE ID(n)=%3$s RETURN l as module",
                    IMAGING_CONTAINS, IMAGING_MODULE_LABEL, n.getId() );

            // List incoming modules
            String forgedFromOtherModules = String.format("MATCH (n)<--()<-[:%1$s]-(l:%2$s) WHERE ID(n)=%3$s RETURN l as module",
                    IMAGING_CONTAINS, IMAGING_MODULE_LABEL, n.getId() );

            Result resTo = neo4jAL.executeQuery(forgedToOtherModules);
            while (resTo.hasNext()) {
                Node resToNode = (Node) resTo.next().get("module");
                Long id = resToNode.getId();

                if( id == nodeModule.getId()) continue;
                toOtherModule.add(resToNode);
            }

            Result resFrom = neo4jAL.executeQuery(forgedFromOtherModules);
            while (resFrom.hasNext()) {
                Node resToNode = (Node) resFrom.next().get("module");
                Long id = resToNode.getId();

                if( id == nodeModule.getId()) continue;
                fromOtherModule.add(resToNode);
            }


        }

        // Merge Link to other Modules
        for (Node toLink : toOtherModule) {
            boolean existsRel = false;
            for(Relationship rels : nodeModule.getRelationships(Direction.OUTGOING, referencesRel)) {
                if(rels.getEndNodeId() == toLink.getId()) existsRel=true;
            }
            if(!existsRel) nodeModule.createRelationshipTo(toLink, referencesRel);
        }

        // Merge Link Modules from other Modules
        for (Node fromLink : fromOtherModule) {
            boolean existsRel = false;
            for(Relationship rels : nodeModule.getRelationships(Direction.INCOMING, referencesRel)) {
                if(rels.getStartNodeId() == fromLink.getId()) existsRel=true;
            }
            if(!existsRel) fromLink.createRelationshipTo(nodeModule, referencesRel);
        }

        // Display info
        List<String> toLevelName =  toOtherModule.stream().map(x -> (String) x.getProperty("Name")).collect(Collectors.toList());
        List<String> fromLevelName =  fromOtherModule.stream().map(x -> (String) x.getProperty("Name")).collect(Collectors.toList());
        neo4jAL.logInfo(toOtherModule.size() + " outgoing relationships were detected and created. To : " + String.join(",", toLevelName));
        neo4jAL.logInfo(fromOtherModule.size() + " incoming relationships were detected and created." + String.join(",", fromLevelName));
    }


    /**
     * Refresh count for modules
     * @param neo4jAL Neo4j access layer
     * @param moduleNode Level node necessitating a
     * @return
     * @throws Neo4jQueryException
     */
    public static Node moduleRecount(Neo4jAL neo4jAL, Node moduleNode) throws Neo4jQueryException {
        // Update the old Level 5 and remove it is there no node linked to it
        String forgedNumConnected = String.format("MATCH (n:%1$s)-[:%2$s]->(o) WHERE ID(n)=%3$s RETURN COUNT(o) as countNode;",
                IMAGING_MODULE_LABEL, IMAGING_CONTAINS, moduleNode.getId());

        Result resNumConnected = neo4jAL.executeQuery(forgedNumConnected);

        Long numLeft = 0L;
        if(resNumConnected.hasNext()) {
            numLeft = (Long) resNumConnected.next().get("countNode");
        }

        // Delete the old module node if it's empty
        if (numLeft == 0) {
            // Detach
            for(Relationship rel : moduleNode.getRelationships()) {
                rel.delete();
            }
            // Delete
            moduleNode.delete();
            neo4jAL.logInfo("Module had no more relationships with objects and was deleted.");
        } else {
            // Update count property
            moduleNode.setProperty(ModuleNode.getCountProperty(), numLeft);
            neo4jAL.logInfo("Module still has " + numLeft + " relationships with objects and will not be deleted.");
        }

        return moduleNode;
    }


    public static Node groupModule(Neo4jAL neo4jAL, String applicationContext, String groupName, List<Node> nodeList) throws Neo4jNoResult, Neo4jQueryException, Neo4jBadNodeFormatException, Neo4jBadRequestException {

        RelationshipType containsRel = RelationshipType.withName(IMAGING_CONTAINS);
        RelationshipType belongToRel = RelationshipType.withName(IMAGING_BELONG_TO);
        Label moduleLabel = Label.label(IMAGING_MODULE_LABEL);
        Label objectLabel = Label.label(IMAGING_OBJECT_LABEL);
        Label subObjectLabel = Label.label(IMAGING_SUB_OBJECT_LABEL);

        // Assert the application name is not empty
        assert !applicationContext.isEmpty() : "The application name cannot be empty.";
        neo4jAL.logInfo(nodeList + " Potential candidates for grouping on module with name : "+groupName);

        // Clean the group name by removing the Demeter Prefix
        groupName = groupName.replaceAll(GROUP_MODULE_TAG_IDENTIFIER, "");

        // Get other modules nodes
        Set<Node> affectedModules = new HashSet<>();
        for(Node n : nodeList) {
            // The node is supposed to be linked to only one module
            Iterator<Relationship> relIt = n.getRelationships(Direction.INCOMING, containsRel).iterator();

            if(!relIt.hasNext()) continue;
            Node modNode = relIt.next().getStartNode();
            // Assert that the node is a module
            if(!modNode.hasLabel(moduleLabel)) continue;

            // Add to set
            affectedModules.add(modNode);
        }

        // Backup Modules
        for(Node mod : affectedModules ) {
            ModuleNode nodeMode = ModuleNode.fromNode(neo4jAL, mod);
            nodeMode.createBackup(applicationContext, nodeList);
        }

        // Merge new module
        String forgedLabel = applicationContext + ":" + IMAGING_MODULE_LABEL;
        String forgedFindModule = String.format("MATCH (o:%1$s) WHERE o.%2$s='%3$s' RETURN o as node;", forgedLabel, IMAGING_MODULE_NAME, groupName);

        Node newModule = null;

        Result result = neo4jAL.executeQuery(forgedFindModule);
        if(result.hasNext()) {
            // Module with same name was found, and results will be merge into it
            newModule = (Node) result.next().get("node");
        } else {
            // Create a new module
            Transaction tx = neo4jAL.getTransaction();
            Label applicationLabel = Label.label(applicationContext);

            newModule = tx.createNode(moduleLabel);
            newModule.addLabel(applicationLabel);

            newModule.setProperty(ModuleNode.getNameProperty(), groupName);
            newModule.setProperty(ModuleNode.getColorProperty(), "rgb(51, 204, 51)"); // Green
            newModule.setProperty(ModuleNode.getTypeProperty(), "module");
            newModule.setProperty(ModuleNode.getAipIdProperty(), "9999999"); // the Node doesn't belong to AIP
        }

        // Treat objects
        // Link all the objects tagged to you modules.
        // SubObjects referring the object should also be linked
        // And SubObjects can also have SubObjects
        int numSubObjects = 0;
        neo4jAL.logInfo("About to re-link " + nodeList.size() + " objects.");

        Set<Long> affectedModuleId = affectedModules.stream().map(Node::getId).collect(Collectors.toSet());

        List<Node> subObjectList = new CopyOnWriteArrayList<>(nodeList);
        Set<Long> visitedNodes = new HashSet<>();
        Set<Long> visitedObjects = new HashSet<>();

        // Treat node in a first pass
        for (Node rObject : nodeList) {

            // Treat sub object
            if(rObject.hasLabel(subObjectLabel)) {
                // Add the object to the node List
                subObjectList.add(rObject);
            }

            // Treat Objects
            if(rObject.hasLabel(objectLabel)) {

                // Remove node relationships to other modules and create new one
                for(Relationship rel : rObject.getRelationships(Direction.INCOMING, containsRel)) {
                    Node startModule = rel.getStartNode();
                    if(affectedModuleId.contains(startModule.getId())) {
                        rel.delete();
                    }
                }

                // Create the contains relationship to the object
                newModule.createRelationshipTo(rObject, containsRel);
                // Apply new Module name

                // If the node has a module property, replace it
                rObject.setProperty(MODULE_PROPERTY, groupName);

                // Get relationship to subObject (only belong_to relationships)
                for(Relationship rel : rObject.getRelationships(belongToRel)) {
                    Node otherNode = rel.getOtherNode(rObject);

                    // If otherNode is a SubObject, and was not already visited
                    if(otherNode.hasLabel(subObjectLabel)){
                        if(!visitedNodes.contains(otherNode.getId())) {
                            subObjectList.add(rObject);
                        } else {
                            continue;
                        }
                    }
                }

                // Add to the visited node list
                visitedNodes.add(rObject.getId());
                visitedObjects.add(rObject.getId());
            }
        }

        Stack<Node> toInvestigate = new Stack<>();

        // Filter SubObjects, verify they're linked to object added
        for(Node subObj : subObjectList) {
            Long id = subObj.getId();
            String forgeReq = String.format("MATCH (n)-[:"+IMAGING_BELONG_TO+"*]->(o:"+IMAGING_OBJECT_LABEL+":"+ applicationContext+ ") WHERE ID(n)=%d RETURN ID(o) as idObj;", id);

            Result res = neo4jAL.executeQuery(forgeReq);
            while (res.hasNext()) {
                Long idObj =  (Long) res.next().get("idObj");
                if(visitedObjects.contains(idObj)) {
                    toInvestigate.add(subObj);
                    break;
                }
            }
        }

        // Investigate only on subObjects linked to nodes
        Set<Long> visited = new HashSet<>();
        while (!toInvestigate.empty()) {
            Node n = toInvestigate.pop();

            // Get relationship to subObject (only belong_to relationships)
            for(Relationship rel : n.getRelationships(belongToRel)) {
                Node otherNode = rel.getOtherNode(n);

                if(!visited.contains(otherNode.getId())) {
                    toInvestigate.add(otherNode);
                }

            }

            // Remove node relationships to other modules and create new one
            for(Relationship rel : n.getRelationships(Direction.INCOMING, containsRel)) {
                Node startModule = rel.getStartNode();
                if(affectedModuleId.contains(startModule.getId())) {
                    rel.delete();
                }
            }

            // Create the contains relationship to the object
            newModule.createRelationshipTo(n, containsRel);

            visited.add(n.getId());
        }


        neo4jAL.logInfo(numSubObjects + " Sub-objects were relinked during the process");

        // Count
        try {
            moduleRecount(neo4jAL, newModule);
            refreshModuleLinks(neo4jAL, newModule);
        } catch (Exception e) {
            neo4jAL.logError("Refresh failed for new module.", e);
        }

        // Update old modules w/ recount
        for(Node mod : affectedModules) {
            try {
                moduleRecount(neo4jAL, mod);
                refreshModuleLinks(neo4jAL, mod);
            } catch (Exception e) {
                neo4jAL.logError("Refresh failed for old module.", e);
            }
        }

        return  newModule;
    }


    public static List<Node> groupAllModules(Neo4jAL neo4jAL, String applicationContext) throws Neo4jQueryException {
        Map<String, List<Node>> groupMap = new HashMap<>();

        neo4jAL.logInfo("Starting Demeter module grouping...");

        // Get the list of nodes prefixed by dm_tag
        String forgedTagRequest = String.format("MATCH (o:%1$s) WHERE any( x in o.%2$s WHERE x CONTAINS '%3$s')  " +
                "WITH o, [x in o.%2$s WHERE x CONTAINS '%3$s'][0] as g " +
                "RETURN o as node, g as group;", applicationContext, IMAGING_OBJECT_TAGS, GROUP_MODULE_TAG_IDENTIFIER);

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
                neo4jAL.logInfo("# Now processing group with name : " +groupName);
                Node n = groupModule(neo4jAL, applicationContext, groupName, nodeList);
                resNodes.add(n);
            } catch (Exception | Neo4jNoResult | Neo4jBadNodeFormatException | Neo4jBadRequestException err) {
                neo4jAL.logError("An error occurred trying to create Level 5 for nodes with tags : " + groupName, err);
            }
        }

        neo4jAL.logInfo("Demeter module grouping finished.");
        neo4jAL.logInfo("Cleaning tags...");

        // Once the operation is done, remove Demeter tag prefix tags
        String removeTagsQuery = String.format("MATCH (o:%1$s) WHERE EXISTS(o.%2$s)  SET o.%2$s = [ x IN o.%2$s WHERE NOT x CONTAINS '%3$s' ] RETURN COUNT(o) as removedTags;",
                applicationContext, IMAGING_OBJECT_TAGS, GROUP_MODULE_TAG_IDENTIFIER);
        Result tagRemoveRes = neo4jAL.executeQuery(removeTagsQuery);

        if(tagRemoveRes.hasNext()) {
            Long nDel = (Long) tagRemoveRes.next().get("removedTags");
            neo4jAL.logInfo( "# " + nDel + " demeter 'module group tags' were removed from the database.");
        }

        neo4jAL.logInfo("Cleaning Done !");

        return resNodes;
    }

}
