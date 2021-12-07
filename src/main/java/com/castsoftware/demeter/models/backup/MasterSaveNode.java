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

package com.castsoftware.demeter.models.backup;

import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import org.neo4j.graphdb.Node;

/**
 * Master save node
 * The member are public by choice ( to be returned as a Neo4j value)
 */
public class MasterSaveNode {
	public Long id;
	public String name;
	public String description;
	public Long timestamp;
	public String picture;

	private Node node;

	public Long getId() {
		return id;
	}


	public String getPicture() {
		return picture;
	}

	/**
	 * Set a new picture format
	 * @param picture value
	 */
	public void setPicture(String picture) {
		this.picture = picture;
		if(node != null) node.setProperty("Picture", picture);
	}

	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name value
	 */
	public void setName(String name) {
		this.name = name;
		if(node != null) node.setProperty("Name", name);
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description value
	 */
	public void setDescription(String description) {
		this.description = description;
		if(node != null) node.setProperty("Description", description);
	}

	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp value
	 * @param timestamp value
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		if(node != null) node.setProperty("Timestamp", timestamp);
	}

	/**
	 * Master Save Constructor
	 * @param name Name of the backup
	 * @param description Description
	 * @param timestamp Timestamp
	 * @param picture Picture
	 */
	public MasterSaveNode(String name, String description, Long timestamp, String picture) {
		this.node = null;
		this.id= -1L;
		this.name = name;
		this.description = description;
		this.timestamp = timestamp;
		this.picture = picture;
	}

	/**
	 * Constructor from a node
	 * @param node Node to treat
	 * @throws Neo4jBadNodeFormatException
	 */
	public MasterSaveNode(Node node) throws Neo4jBadNodeFormatException {
		try {
			this.node = node;
			this.id = (Long) node.getId();
			this.name = (String) node.getProperty("Name");
			this.description =  (String) node.getProperty("Description");
			this.timestamp =  (Long) node.getProperty("Timestamp");
			this.picture =  (String) node.getProperty("Picture");
		} catch (Exception e) {
			throw new Neo4jBadNodeFormatException(String.format("The MasterSaveNode with id [%d] is not in a correct format", node.getId()), "MASTxCONS01");
		}
	}
}
