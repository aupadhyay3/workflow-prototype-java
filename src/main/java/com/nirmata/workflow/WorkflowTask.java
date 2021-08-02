package com.nirmata.workflow;

public interface WorkflowTask {
    String getTaskType();

    void execute();
}
