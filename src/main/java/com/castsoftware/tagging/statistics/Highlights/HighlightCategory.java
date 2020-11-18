package com.castsoftware.tagging.statistics.Highlights;

public enum HighlightCategory {
    NOT_RELEVANT("Not relevant use cases"),
    QUICK_WINS("Quick wins"),
    MID_TERM("Mid Term bets"),
    LONG_TERM("Long term bets");

    String text;

    public String getText() {
        return this.text;
    }

    HighlightCategory(String text) {
        this.text = text;
    }

}
