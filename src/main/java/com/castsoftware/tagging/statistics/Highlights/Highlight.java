package com.castsoftware.tagging.statistics.Highlights;

import com.castsoftware.tagging.config.Configuration;



public class Highlight {

    private static final Integer LONG_TERM_BREAKPOINT = Integer.parseInt(Configuration.get("statistics.highlight.long_term_breakpoint"));
    private static final Integer MID_TERM_BREAKPOINT = Integer.parseInt(Configuration.get("statistics.highlight.mid_term_breakpoint"));
    private static final Integer QUICK_WIN_BREAKPOINT = Integer.parseInt(Configuration.get("statistics.highlight.quick_wins_breakpoint"));

    private String title;
    private HighlightCategory category;
    private HighlightType type;
    private String useCaseTitle;
    private String description;
    private Integer findings;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(HighlightType type) {
        this.type = type;
    }

    public HighlightCategory getCategory() {
        return category;
    }

    public void setCategory(HighlightCategory category) {
        this.category = category;
    }

    public String getUseCaseTitle() {
        return useCaseTitle;
    }

    public HighlightType getType() {
        return type;
    }

    public void setUseCaseTitle(String useCaseTitle) {
        this.useCaseTitle = useCaseTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFindings() {
        return findings;
    }

    public void setFindings(Integer findings) {
        this.findings = findings;
    }

    private void computeCategory() {
        if(findings < QUICK_WIN_BREAKPOINT) {
            category = HighlightCategory.NOT_RELEVANT;
            return;
        }

        if(findings > LONG_TERM_BREAKPOINT) {
            category = HighlightCategory.LONG_TERM;
        } else if (findings > MID_TERM_BREAKPOINT) {
            category = HighlightCategory.MID_TERM;
        } else {
            category = HighlightCategory.QUICK_WINS;
        }
    }

    public Highlight(String title, String useCaseTitle, String description, Integer findings, HighlightType type) {
        this.title = title;
        this.type = type;
        this.useCaseTitle = useCaseTitle;
        this.description = description;
        this.findings = findings;

        computeCategory();
    }

}
