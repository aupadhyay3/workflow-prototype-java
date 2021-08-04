package com.nirmata.workflow.task;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TaskWatcher {
    private static final Logger logger = LoggerFactory.getLogger(TaskWatcher.class);
    private final Map<String, TaskExecutor> executors;
    private final RawCustomResourceOperationsImpl api;

    public TaskWatcher(RawCustomResourceOperationsImpl api, Map<String, TaskExecutor> executors) {
        this.api = api;
        this.executors = executors;
    }

    public void start() {
        try {
            api.watch(new Watcher<String>() {
                @Override
                public void eventReceived(Action action, String resource) {
                    if (action == Action.ADDED) {
                        JSONObject json = new JSONObject(resource);
                        String taskType = json.getJSONObject("spec").getString("type");
                        
                        if (executors.containsKey(taskType)) {
                            TaskExecutor executor = executors.get(taskType);
                            executor.addTask(json);
                        } else {
                            logger.error("No executor exists for task type {}", taskType);
                        }
                    }
                }

                @Override
                public void onClose(WatcherException e) {
                    logger.info("Closing watch");
                    if (e != null) {
                        logger.info(e.getMessage());
                    }
                }
            });
            logger.info("Watch started");

        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
        }
    }
}
