package com.nirmata.workflow.crd;

public class WorkflowTaskSpec {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "WorkflowTaskSpec{type=" + type + "}";
    }
}
