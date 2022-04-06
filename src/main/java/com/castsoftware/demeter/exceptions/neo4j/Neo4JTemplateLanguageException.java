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

public class Neo4JTemplateLanguageException extends TaggingException {
    private static final long serialVersionUID = -257426673589618244L;
    private static final String MESSAGE_PREFIX =
            "Error, Neo4j Template Language produced is not correct : ";
    private static final String CODE_PREFIX = "NEO_TL_";

    public Neo4JTemplateLanguageException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public Neo4JTemplateLanguageException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

    public Neo4JTemplateLanguageException(String message, String request, String code) {
        super(
                MESSAGE_PREFIX.concat(message).concat(". Query : ").concat(request),
                CODE_PREFIX.concat(code));
    }
}
