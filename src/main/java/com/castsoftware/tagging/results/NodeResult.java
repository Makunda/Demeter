package com.castsoftware.tagging.results;

import org.neo4j.graphdb.Node;

public class NodeResult {

    public Node node;

    public NodeResult(Node node) {
        this.node = node;
    }
}
