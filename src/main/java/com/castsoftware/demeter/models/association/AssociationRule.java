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

package com.castsoftware.demeter.models.association;

import com.castsoftware.demeter.database.Neo4jAL;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.demeter.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.demeter.models.Neo4jObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.Map;

// TODO Continue here to develop the rules
public class AssociationRule extends Neo4jObject {

	private static String ERROR_PREFIX = "ARULx";

	// Label & statics properties
	private static String LABEL = "AssociationRule";
	private static String SOURCE_TYPE_PROPERTY = "SourceType";
	private static String DEST_TYPE_PROPERTY = "SourceType";
	private static String TO_CONFIG = "BelongsTo";

	private String sourceType;
	private  String destType;

	// Neo4j related
	private Node node;

	public static String getLabel() {
		return LABEL;
	}

	public static String getSourceTypeProperty() {
		return SOURCE_TYPE_PROPERTY;
	}

	public static String getDestTypeProperty() {
		return DEST_TYPE_PROPERTY;
	}

	public String getSourceType() {
		return sourceType;
	}

	public String getDestType() {
		return destType;
	}

	@Override
	public Node createNode() throws Neo4jBadRequestException, Neo4jNoResult {
		String req  = String.format("MERGE (n:`%1$s` { %2$s: $sourceType, %3$s: $destType }) RETURN n as node;",
				getLabel(), getSourceTypeProperty(), getDestTypeProperty());
		Map<String, Object> params = Map.of(getSourceTypeProperty(), this.sourceType, getDestTypeProperty(),this.destType);

		try {
			Result res = this.neo4jAL.executeQuery(req, params);

			if(!res.hasNext()) throw new Neo4jNoResult("The creation request faield to instantiate the node", req, ERROR_PREFIX+"CREN01");


			return (Node) res.next().get("node");
		} catch (Neo4jQueryException e) {
			throw new Neo4jBadRequestException(String.format("The request creating an association rule is not valid. Request: %s", req) , e, ERROR_PREFIX+"CREN02");
		}
	}


	/**
	 * Instantiate an association rule from a node
	 * @param neo4jAL Neo4j Access Layer
	 * @param node Node to convert
	 * @throws Neo4jBadNodeFormatException if the node is node correctly formatted
	 */
	public AssociationRule(Neo4jAL neo4jAL, Node node) throws Neo4jBadNodeFormatException{
		super(neo4jAL);

		this.node = node;
		try {
			this.destType = (String) node.getProperty(AssociationRule.getDestTypeProperty());
			this.sourceType = (String) node.getProperty(AssociationRule.getSourceTypeProperty());
		} catch (Exception e) {
			throw new Neo4jBadNodeFormatException("Failed to instanciate an association rule node", e, ERROR_PREFIX+"ARUL01");
		}
	}

	/**
	 * Instantiate an association rule with its source and dest type
	 * @param neo4jAL Neo4j Access Layer
	 * @param sourceType
	 * @param destType
	 */
	public AssociationRule(Neo4jAL neo4jAL, String sourceType, String destType) {
		super(neo4jAL);
		this.sourceType = sourceType;
		this.destType = destType;
	}
}
