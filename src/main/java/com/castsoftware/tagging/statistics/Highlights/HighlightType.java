package com.castsoftware.tagging.statistics.Highlights;

public enum HighlightType {
    TAG("Tag Generator"),
    DOCUMENT("Document Generator");

    String text;
    public String getText() {
        return this.text;
    }
    HighlightType(String text) {
        this.text = text;
    }
}
