package com.nirmata.workflow;

public interface WorkflowTask {
    String getType();

    void execute() throws Exception;
}
