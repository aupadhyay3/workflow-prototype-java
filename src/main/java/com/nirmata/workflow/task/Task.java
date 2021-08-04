package com.nirmata.workflow.task;

public interface Task {
    public void execute() throws Exception;

    public String getType();
}
