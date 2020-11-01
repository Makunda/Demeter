package com.castsoftware.tagging.results;

import com.castsoftware.tagging.models.UseCaseNode;

public class UseCasesMessage {
    public String name;
    public Boolean active;
    public Long id;

    public UseCasesMessage(String name, Boolean active,  Long id) {
        super();
        this.name = name;
        this.active = active;
        this.id = id;
    }

    public UseCasesMessage(UseCaseNode n) {
        super();
        this.name = n.getName();
        this.active = n.getActive();
        this.id = n.getNodeId();
    }
}
