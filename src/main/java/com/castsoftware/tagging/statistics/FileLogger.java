package com.castsoftware.tagging.statistics;

import com.castsoftware.tagging.config.Configuration;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class FileLogger {

    private static final String FILE_LOCATION = Configuration.get("statistics.file.path");
    private static final String FILE_EXTENSION = Configuration.get("statistics.file.extension");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    // Object Properties
    private static final String OBJECT_LABEL = Configuration.get("imaging.node.object.label");
    private static final String OBJECT_EXTERNAL_PROP = Configuration.get("imaging.node.object.external");
    private static final String OBJECT_TYPE_PROP = Configuration.get("imaging.node.object.type");


    private static final String OBJECTPROPERTY_DESCRIPTION_PROP = Configuration.get("imaging.node.objectProperty.description");
    private static final String OBJECT_PROPERTY_RELATIONSHIP = Configuration.get("imaging.link.object_property");
    private static final String OBJECT_PROPERTY_RELATIONSHIP_VAL = Configuration.get("imaging.link.object_property.value");


    private static FileLogger SINGLETON = new FileLogger();
    private Map<String, JSONArray> stats = new HashMap<>();

    public static FileLogger getLogger() {
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

    public static JSONObject nodeToJOSNStats (Node n) {
        JSONObject nodeDetails = new JSONObject();
        RelationshipType objectToPropertyRel = RelationshipType.withName(OBJECT_PROPERTY_RELATIONSHIP);
        Label objectLabel = Label.label(OBJECT_LABEL);

        // Add nodes information and properties
        Map<String, Object> properties = n.getAllProperties();
        nodeDetails.putAll(properties);

        // Add properties
        /*
        Map<String, Object> objectProps = new HashMap<>();
        for(Relationship rel : n.getRelationships(Direction.OUTGOING, objectToPropertyRel)) {
            try {
                Node otherNode = rel.getEndNode();
                String propName = (String) otherNode.getProperty(OBJECTPROPERTY_DECRIPTION_PROP);
                Object value = rel.getProperty(OBJECT_PROPERTY_RELATIONSHIP_VAL);
                objectProps.put(propName, value);
            } catch (Exception ignored) {

            }
        }
        nodeDetails.putAll(objectProps); */

        // Add Callers links
        //nodeDetails.put("Callers Relationships", getLinkedObjectsStats(n, Direction.INCOMING));

        // Add Callees links
        //nodeDetails.put("Callees Relationships", getLinkedObjectsStats(n, Direction.OUTGOING));

        return nodeDetails;
    }


    public void write() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String forgedPath = FILE_LOCATION + "Statistics_" + sdf.format(timestamp) + FILE_EXTENSION;

        try (FileWriter file = new FileWriter(forgedPath)) {
            file.write(JSONObject.toJSONString(stats));
            file.flush();
            stats.clear();
        }

    }

    private FileLogger() {
    }

}
