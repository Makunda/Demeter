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

import com.castsoftware.demeter.utils.configuration.LevelGroupingConfiguration;
import com.castsoftware.demeter.utils.configuration.ObjectConfiguration;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

public class ObjectService {

	/**
	 * Verify that the node is a level from 1 to 5
	 * @param n Node to verify
	 * @return True if the node is a level, False otherwise
	 */
	public static Boolean isObject(Node n) {
		return n.hasLabel(ObjectConfiguration.getObjectLabel());
	}

	/**
	 * Apply the
	 * @param n Node to process
	 * @param originalLevel Name of the original level
	 */
	public static void applyOriginalLevel(Node n, String originalLevel) {
		n.setProperty(LevelGroupingConfiguration.getExtensionOriginalLevelIdentifier(), originalLevel);
	}

	/**
	 * Apply a level
	 * @param object Object node to process
	 * @param level Node Level to apply
	 */
	public static void applyLevel(Node object, Node level) {
		String name = LevelService.getLevelName(level);
		object.setProperty(ObjectConfiguration.getLevelProperty(), name);
	}


	public ObjectService() {

	}
}
