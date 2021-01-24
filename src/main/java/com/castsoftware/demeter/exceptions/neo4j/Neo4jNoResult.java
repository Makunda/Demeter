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
 * The <code>Neo4jNoResult</code> is thrown when a request doesn't return expected results or if
 * results are empty. Neo4jNoResult
 */
public class Neo4jNoResult extends TaggingException {

  private static final long serialVersionUID = 8218353918930322258L;
  private static final String MESSAGE_PREFIX = "Error, the query returned no results : ";
  private static final String CODE_PREFIX = "NEO_NR_";

  public Neo4jNoResult(String message, Throwable cause, String code) {
    super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
  }

  public Neo4jNoResult(String message, String query, String code) {
    super(
        MESSAGE_PREFIX.concat(message).concat(" . Query : ").concat(query),
        CODE_PREFIX.concat(code));
  }

  public Neo4jNoResult(String message, String query, Throwable cause, String code) {
    super(
        MESSAGE_PREFIX.concat(message).concat(" . Query : ").concat(query),
        cause,
        CODE_PREFIX.concat(code));
  }
}
