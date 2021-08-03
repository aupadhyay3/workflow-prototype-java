package com.nirmata.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

public class TestTask implements WorkflowTask {
    private static final String TaskType = "testType";
    private static final Logger logger = LoggerFactory.getLogger(TestTask.class);

    @Override
    public void execute() throws Exception {
        logger.info("Executing test task...");
        
        TimeUnit.SECONDS.sleep(5);

        logger.info("Completed test task");
    }

    @Override
    public String getType() {
        return TaskType;
    }
}
