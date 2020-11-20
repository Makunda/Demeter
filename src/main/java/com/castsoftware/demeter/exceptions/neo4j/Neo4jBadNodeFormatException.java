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

package com.castsoftware.demeter.exceptions.neo4j;

import com.castsoftware.demeter.exceptions.TaggingException;

/**
 * The <code>Neo4jBadNodeFormat</code> is thrown when the conversion from a Neo4J node to its associated Java model fail.
 * Neo4jBadNodeFormat
 */
public class Neo4jBadNodeFormatException extends TaggingException {
    private static final long serialVersionUID = -1234489315467963030L;
    private static final String MESSAGE_PREFIX = "Error, the node is not in a good format. ";
    private static final String CODE_PREFIX = "NEO_BF_";

    public Neo4jBadNodeFormatException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public Neo4jBadNodeFormatException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

}
