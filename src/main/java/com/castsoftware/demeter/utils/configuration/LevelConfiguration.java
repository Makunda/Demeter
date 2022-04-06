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
import jdk.management.jfr.ConfigurationInfo;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class LevelConfiguration {

	/**
	 * Get the aggregates relationships from a level to an object
	 * @return The relationship
	 */
	public static RelationshipType getAggregatesRelationship() {
		String relType = Configuration.get("imaging.node.level_nodes.links");
		return RelationshipType.withName(relType);
	}

	/**
	 * Get the imaging label of Level
	 * @return The label level
	 */
	public static Label getLevelLabel(Integer number) {
		String label = Configuration.get("imaging.node.level.base_label");
		String fullLabel = String.format("%s%d", label, number);
		return Label.label(fullLabel);
	}

	/**
	 * Get base label
	 * @return The base label
	 */
	public static String getBaseLabel() {
		return Configuration.get("imaging.node.level.base_label");
	}


	/**
	 * Get the name of the level
	 * @return The Name property
	 */
	public static String getLevelNameProperty() {
		return Configuration.get("imaging.node.level.name");
	}

	/**
	 * Get the full name of the level
	 * @return The base label
	 */
	public static String getLevelFullNameProperty() {
		return Configuration.get("imaging.node.level.fullName");
	}

}
