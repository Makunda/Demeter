package com.castsoftware.tagging.exceptions.neo4j;

import com.castsoftware.tagging.exceptions.TaggingException;

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
