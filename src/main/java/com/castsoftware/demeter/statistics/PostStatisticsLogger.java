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

package com.castsoftware.demeter.statistics;

import com.castsoftware.demeter.config.Configuration;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class PostStatisticsLogger {

    private static final String FILE_LOCATION = Configuration.get("statistics.file.path");
    private static final String FILE_EXTENSION = Configuration.get("statistics.file.extension");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    // Object Properties
    private static final String OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String OBJECT_EXTERNAL_PROP = Configuration.get("imaging.node.object.external");
    private static final String OBJECT_TYPE_PROP = Configuration.get("imaging.node.object.type");


    private static final String OBJECT_PROPERTY_DESCRIPTION_PROP = Configuration.get("imaging.node.objectProperty.description");
    private static final String OBJECT_PROPERTY_RELATIONSHIP = Configuration.get("imaging.link.object_property");
    private static final String OBJECT_PROPERTY_RELATIONSHIP_VAL = Configuration.get("imaging.link.object_property.value");


    private static PostStatisticsLogger SINGLETON = new PostStatisticsLogger();
    private Map<String, JSONArray> stats = new HashMap<>();

    public static PostStatisticsLogger getLogger() {
        return SINGLETON;
    }

    public void addStatToTag(String tag, JSONObject json) {
        JSONArray jsonArr = stats.getOrDefault(tag, new JSONArray());
        jsonArr.add(json);
        stats.put(tag, jsonArr);
    }

    public static String getLinkedObjectsStats(Node n, Direction direction) {

        Label objectLabel = Label.label(OBJECT_LABEL);

        Map<String, JSONArray> callersLinks = new HashMap<>();
        for(Relationship rel : n.getRelationships(direction)) {
            String relName = rel.getType().name();

            try {
                JSONObject relProperties = new JSONObject();

                Node otherNode = rel.getEndNode();

                // Skip if it's not an object Node
                if(otherNode.hasLabel(objectLabel)) continue;

                relProperties.put(OBJECT_TYPE_PROP, otherNode.getProperty(OBJECT_TYPE_PROP));
                relProperties.put(OBJECT_EXTERNAL_PROP, otherNode.getProperty(OBJECT_EXTERNAL_PROP));
                // Add the values of this relation to the CallerLinks table
                JSONArray json = callersLinks.getOrDefault(relName, new JSONArray());
                json.add(relProperties);

                if(callersLinks.containsValue(json)) {

                }

            } catch (Exception ignored) {

            }
        }

        return JSONObject.toJSONString(callersLinks);
    }

    public static JSONObject nodeToJSONStats(Node n) {
        JSONObject nodeDetails = new JSONObject();
        RelationshipType objectToPropertyRel = RelationshipType.withName(OBJECT_PROPERTY_RELATIONSHIP);
        Label objectLabel = Label.label(OBJECT_LABEL);

        // Add nodes information and properties
        Map<String, Object> properties = n.getAllProperties();
        nodeDetails.putAll(properties);

        // Add properties


        return nodeDetails;
    }

    public void addParagraph(String title, String content) {

    }

    /**
     * Write properties buffer to file.
     * @throws IOException
     */
    public void write() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String forgedPath = FILE_LOCATION + "Statistics_" + sdf.format(timestamp) + FILE_EXTENSION;

        try (FileWriter file = new FileWriter(forgedPath)) {
            file.write(JSONObject.toJSONString(stats));
            file.flush();
            stats.clear();
        }

    }

    private PostStatisticsLogger() {
    }

}
