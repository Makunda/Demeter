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

package com.castsoftware.demeter.utils.configuration;

import com.castsoftware.demeter.config.Configuration;
import org.neo4j.graphdb.Label;

public class ObjectConfiguration {

	/**
	 * Get the label of the object
	 * @return The label of the object
	 */
	public static Label getObjectLabel() {
		String labelAsString = Configuration.get("imaging.node.object.label");
		return Label.label(labelAsString);
	}

	public static Label getOriginalLevelName() {
		String labelAsString = Configuration.get("imaging.node.object.label");
		return Label.label(labelAsString);
	}

	/**
	 * Get level property of the object
	 * @return The level property
	 */
	public static String getLevelProperty() {
		return Configuration.get("imaging.node.object.level");
	}

}
