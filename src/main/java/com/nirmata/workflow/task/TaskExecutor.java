package com.nirmata.workflow.task;

import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Map;
import java.util.UUID;

public class TaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    private final BlockingQueue<JSONObject> workQueue = new LinkedBlockingQueue<>();
    private final Task task;
    private final int threadPoolSize;
    private String podName;

    private RawCustomResourceOperationsImpl api;

    public TaskExecutor(Task task, int threadPoolSize) {
        this.task = task;
        this.threadPoolSize = threadPoolSize;
        try {
            podName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error(e.getStackTrace().toString());
            podName = "unknown";
        }
    }

    public void start(RawCustomResourceOperationsImpl api) {
        this.api = api;
        for (int i = 0; i < threadPoolSize; i++) {
            Thread t = new ExecutorThread();
            t.start();
        }
    }

    public void addTask(JSONObject json) {
        workQueue.add(json);
    }

    private class ExecutorThread extends Thread {
        @Override
        public void run() {
            String executorName = podName + "-" + getId();
            int taskCount = 0;

            while (true) {
                try {
                    JSONObject json = workQueue.poll();

                    if (json != null) {
                        String taskName = json.getJSONObject("metadata").getString("name");
                        RawCustomResourceOperationsImpl cr = api.withName(taskName);

                        if (json.isNull("status") || json.getJSONObject("status").isNull("executor")) {
                            JSONObject updates = new JSONObject();

                            JSONObject status;
                            if (json.isNull("status")) {
                                status = new JSONObject();
                            } else {
                                status = json.getJSONObject("status");
                            }
                            updates.put("status", status);

                            //set executor field
                            status.put("executor", executorName);

                            //update state to executing
                            status.put("state", TaskExecutionState.EXECUTING.toString());
                            cr.updateStatus(updates.toString());

                            try {
                                String taskType = task.getType();
                                logger.debug("Task {} of type {} executing...", taskName, taskType);

                                // execute task
                                task.execute();

                                taskCount += 1;
                                logger.debug("Task {} of type {} completed. Executor total: {}", taskName, taskType, taskCount);

                                //update state to completed
                                status.put("state", TaskExecutionState.COMPLETED.toString());
                                Map<String, Object> result = cr.updateStatus(updates.toString());

                                logger.debug("Updated resource: {}", result);

                            } catch (Exception e) {
                                logger.error("Task {} failed with exception {}", taskName, e);

                                status.put("state", TaskExecutionState.FAILED.toString());
                                cr.updateStatus(updates.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getStackTrace().toString());
                }
            }
        }
    }
}
