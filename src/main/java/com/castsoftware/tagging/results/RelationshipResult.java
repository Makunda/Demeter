package com.castsoftware.tagging.results;

import org.neo4j.graphdb.Relationship;

public class RelationshipResult {

    public Relationship relationship;

    public RelationshipResult(Relationship relationship) {
        this.relationship = relationship;
    }
}
