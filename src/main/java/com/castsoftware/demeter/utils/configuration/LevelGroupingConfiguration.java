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

/**
 * Level Configuration
 */
public class LevelGroupingConfiguration {

	/**
	 * Get the extension level identifier property to apply on levels
	 * @return Property
	 */
	public static String getExtensionLevelIdentifier() {
		return Configuration.get("demeter.property.generated_level");
	}

	/**
	 * Get the Original leve identifier to be applied on objects moved
	 * @return Property
	 */
	public static String getExtensionOriginalLevelIdentifier() {
		return Configuration.get("demeter.property.original_group");
	}
}
