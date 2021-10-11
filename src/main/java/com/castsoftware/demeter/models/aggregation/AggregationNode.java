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

package com.castsoftware.demeter.models.aggregation;

public class AggregationNode {
	private long aggregationDepth;
	private boolean published;
	private String createdBy;
	private String[] collaborators;
	private String name;

	public long getAggregationDepth() {
		return aggregationDepth;
	}

	public void setAggregationDepth(long aggregationDepth) {
		this.aggregationDepth = aggregationDepth;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String[] getCollaborators() {
		return collaborators;
	}

	public void setCollaborators(String[] collaborators) {
		this.collaborators = collaborators;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
