/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.demeter.database;

import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.List;

public class Neo4jTypeManager {

    /**
     * Get the value of a parameter as Double. If another value is detected, it will re-write the
     * parameter of the node.
     *
     * @param node     Node containing the value to be extracted
     * @param property Name of the property
     * @return Value of the return
     */
    public static Double getAsDouble(Node node, String property, Double defaultValue) {
        Double val = defaultValue;
        if (!node.hasProperty(property)) {
            node.setProperty(property, val);
            return val;
        }
        Object valObject = node.getProperty(property);

        if (valObject instanceof Double) {
            return (Double) valObject;
        }

        if (valObject instanceof Float) {
            Float aFloat = (Float) node.getProperty(property);
            val = aFloat.doubleValue();
            node.setProperty(property, val);
            return val;
        }

        if (valObject instanceof Long) {
            Long aLong = (Long) node.getProperty(property);
            val = aLong.doubleValue();
            node.setProperty(property, val);
        }

        if (valObject instanceof Integer) {
            Integer aInteger = (Integer) node.getProperty(property);
            val = aInteger.doubleValue();
            node.setProperty(property, val);
            return val;
        }

        if (valObject instanceof String) {
            String aString = (String) node.getProperty(property);
            val = Double.parseDouble(aString);
            node.setProperty(property, val);
        }

        node.setProperty(property, val);
        return val;
    }

    /**
     * Get the value of a parameter as Long. If another value is detected, it will re-write the
     * parameter of the node.
     *
     * @param node     Node containing the value to be extracted
     * @param property Name of the property
     * @return Value of the return
     */
    public static Long getAsLong(Node node, String property, Long defaultValue) {
        if (!node.hasProperty(property)) {
            node.setProperty(property, defaultValue);
            return defaultValue;
        }

        Object valObject = node.getProperty(property);
        Long res = getAsLong(valObject, defaultValue);
        node.setProperty(property, res);

        return res;
    }

    /**
     * Get a value as a Long
     *
     * @param valObject    Value object to treat from Neo4j
     * @param defaultValue Default value if the value is not compliant
     * @return The Long found in the object, or the default value
     */
    public static Long getAsLong(Object valObject, Long defaultValue) {
        long val;

        if (valObject instanceof Long) {
            val = (Long) valObject;
            return val;
        }

        if (valObject instanceof Float) {
            Float aFloat = (Float) valObject;
            val = aFloat.longValue();
            return val;
        }

        if (valObject instanceof Double) {
            Double aDouble = (Double) valObject;
            val = aDouble.longValue();
            return val;
        }

        if (valObject instanceof Integer) {
            Integer aInteger = (Integer) valObject;
            val = aInteger.longValue();
            return val;
        }

        if (valObject instanceof String) {
            try {
                String aString = (String) valObject;
                val = Long.parseLong(aString);
                return val;
            } catch (Exception ignored) {
                // return the default value if this last option fail
            }
        }

        return defaultValue;
    }

    public static Boolean getAsBoolean(Node node, String property, Boolean defaultValue) {
        Boolean val = defaultValue;

        if (!node.hasProperty(property)) {
            node.setProperty(property, val);
            return val;
        }

        Object valObject = node.getProperty(property);

        if (valObject instanceof Boolean) {
            return (Boolean) valObject;
        }

        if (valObject instanceof String) {
            String aString = (String) node.getProperty(property);
            val = Boolean.parseBoolean(aString);
            node.setProperty(property, val);
            return val;
        }

        node.setProperty(property, val);
        return val;
    }

    /**
     * Get the value of the property as a String
     *
     * @param node         Node concerned by the property extraction
     * @param property     Property
     * @param defaultValue Default Value
     * @return
     */
    public static String getAsString(Node node, String property, String defaultValue) {
        String val = defaultValue;

        if (!node.hasProperty(property)) {
            node.setProperty(property, val);
            return val;
        }

        Object valObject = node.getProperty(property);

        if (valObject instanceof String) {
            String aString = (String) node.getProperty(property);
            return aString;
        }

        node.setProperty(property, val);
        return val;
    }

    /**
     * Get the property as a list of string
     *
     * @param node
     * @param property
     * @return
     */
    public static List<String> getAsStringList(Node node, String property) {
        String[] strings = new String[0];
        if (!node.hasProperty(property)) {
            node.setProperty(property, strings);
            return Arrays.asList(strings);
        }

        Object valObject = node.getProperty(property);

        try {
            strings = (String[]) valObject;
            return Arrays.asList(strings);
        } catch (ClassCastException ignored) {
        }

        if (valObject instanceof String) {
            strings = Arrays.asList((String) valObject).toArray(new String[0]);
            node.setProperty(property, strings);
            return Arrays.asList(strings);
        }

        node.setProperty(property, strings);
        return Arrays.asList(strings);
    }
}
