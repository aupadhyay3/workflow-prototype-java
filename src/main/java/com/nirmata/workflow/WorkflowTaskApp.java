package com.nirmata.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowTaskApp {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowTaskApp.class);

    public void startApp() {
        WorkflowTaskWatch watch = new WorkflowTaskWatch();
        watch.startWatch("default");
    }

    public static void main(String[] args) {
        WorkflowTaskApp workflowTaskApp = new WorkflowTaskApp();
        workflowTaskApp.startApp();
    }
}