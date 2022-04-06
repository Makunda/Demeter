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

package com.castsoftware.demeter.services.levels;

import com.castsoftware.demeter.utils.configuration.LevelConfiguration;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;
import java.util.Optional;

public class LevelService {

	/**
	 * Get the name of the level linked to the object
	 * @param object Object to investigate
	 * @return The Op
	 */
	public static Optional<Node> getObjectLevel(Node object) {
		Iterator<Relationship> relationships = object.getRelationships(Direction.INCOMING, LevelConfiguration.getAggregatesRelationship()).iterator();

		Node n = null;
		// Parse the relationships, the object is only supposed to have 1 and only 1 link to another level
		while(relationships.hasNext()) {
			// Get the level node
			n = relationships.next().getStartNode();
			// Verify if the node is a level or not
			if(n.hasLabel(LevelConfiguration.getLevelLabel(5))) {
				return Optional.of(n);
			}
		}
		// Default choice
		return Optional.empty();
	}

	/**
	 * Verify that the node is a level from 1 to 5
	 * @param n Node to verify
	 * @return True if the node is a level, False otherwise
	 */
	public static Boolean isLevel(Node n) {
		String baseLabel = LevelConfiguration.getBaseLabel();
		for (Label l : n.getLabels()) {
			if(l.name().startsWith(baseLabel)) return true;
		}

		return false;
	}

	/**
	 * Verify if the node belongs to a named level node
	 * @param n Node to investigate
	 * @param depth Depth of the node
	 * @return True if the node belongs to a specific label
	 */
	public static Boolean isLevel(Node n, Integer depth) {
		Label namedLabel = LevelConfiguration.getLevelLabel(depth);
		return n.hasLabel(namedLabel);
	}

	/**
	 * Get the name of the level
	 * @param n Node to check
	 * @return The name of the level
	 */
	public static String getLevelName(Node n) {
		assert isLevel(n) : "The node is not a level";
		return (String) n.getProperty(LevelConfiguration.getLevelNameProperty());
	}

	/**
	 * Get the name of the level
	 * @param n Node to check
	 * @return The name of the level
	 */
	public static String getLevelFullName(Node n) {
		assert isLevel(n) : "The node is not a level";
		return (String) n.getProperty(LevelConfiguration.getLevelFullNameProperty());
	}

	/**
	 * Link objects to the level
	 * @param object Object to link
	 * @param level5 Target level
	 */
	public static Relationship linkObjectToLevel(Node object, Node level5) {
		assert ObjectService.isObject(object) : "The first parameter needs to be an Imaging Object";
		assert isLevel(level5, 5) : "The first second needs to be an Imaging level 5";

		return level5.createRelationshipTo(object, LevelConfiguration.getAggregatesRelationship());
	}

	/**
	 * Detach all the levels from the node
	 * @param object Object to detach
	 */
	public static void detachLevels(Node object) {
		assert ObjectService.isObject(object) : "The node needs to be an Imaging Object";
		for(Relationship rel: object.getRelationships(Direction.INCOMING, LevelConfiguration.getAggregatesRelationship())) {
			rel.delete();
		}
	}


	public LevelService() {
	}
}
