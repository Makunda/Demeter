package com.castsoftware.tagging.exceptions.neo4j;

import com.castsoftware.tagging.exceptions.TaggingException;

public class Neo4JTemplateLanguageException extends TaggingException {
    private static final long serialVersionUID = -257426673589618244L;
    private static final String MESSAGE_PREFIX = "Error, Neo4j Template Language produced is not correct : ";
    private static final String CODE_PREFIX = "NEO_TL_";

    public Neo4JTemplateLanguageException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public Neo4JTemplateLanguageException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

    public Neo4JTemplateLanguageException(String message, String request, String code) {
        super(MESSAGE_PREFIX.concat(message).concat(". Query : ").concat(request), CODE_PREFIX.concat(code));
    }

}
