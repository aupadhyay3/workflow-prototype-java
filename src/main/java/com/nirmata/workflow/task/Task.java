package com.nirmata.workflow.task;

import com.nirmata.workflow.crd.WorkflowTask;

public interface Task {
    public void execute(WorkflowTask resource) throws Exception;

    public String getType();
}
