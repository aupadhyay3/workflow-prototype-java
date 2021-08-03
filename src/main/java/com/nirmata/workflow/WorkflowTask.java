package com.nirmata.workflow;

public interface WorkflowTask {
    public void execute() throws Exception;

    public String getType();
}
