/*
 *  Copyright (C) 2020  Hugo JOBY
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.castsoftware.tagging.exceptions.neo4j;

import com.castsoftware.tagging.exceptions.TaggingException;

/**
 * The <code>Neo4jBadRequest</code> is thrown when the Neo4j request fails.
 * The failed request can be found in logs.
 * Neo4jBadRequest
 */
public class Neo4jBadRequestException extends TaggingException {
    private static final long serialVersionUID = -4369489315467963030L;
    private static final String MESSAGE_PREFIX = "Error during Neo4j request : ";
    private static final String CODE_PREFIX = "NEO_BR_";

    public Neo4jBadRequestException(String request, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(request), cause, CODE_PREFIX.concat(code));
    }

    public Neo4jBadRequestException(String request, String code) {
        super(MESSAGE_PREFIX.concat(request), CODE_PREFIX.concat(code));
    }

    public Neo4jBadRequestException(String request, String query, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(request).concat(" . Query : ").concat(query), cause, CODE_PREFIX.concat(code));
    }
}
