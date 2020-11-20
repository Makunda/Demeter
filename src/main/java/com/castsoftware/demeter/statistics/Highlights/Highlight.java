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

package com.castsoftware.demeter.statistics.Highlights;

import com.castsoftware.demeter.config.Configuration;



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
