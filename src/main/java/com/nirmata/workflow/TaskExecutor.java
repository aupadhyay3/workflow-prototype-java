package com.nirmata.workflow;

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
    private final WorkflowTask task;
    private String podName;
    private final int threadPoolSize;

    private RawCustomResourceOperationsImpl api;

    public TaskExecutor(WorkflowTask task, int threadPoolSize) {
        this.task = task;
        this.threadPoolSize = threadPoolSize;
        try {
            podName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void start(RawCustomResourceOperationsImpl api) {
        this.api = api;
        for (int i = 0; i < threadPoolSize; i++) {
            Thread t = new Thread(new ExecutorThread());
            t.start();
        }
    }

    public void addTask(JSONObject json) {
        workQueue.add(json);
    }

    private class ExecutorThread implements Runnable {
        private final UUID threadID = UUID.randomUUID();
        private final String executorName = podName + "-" + threadID;
        private int taskCount = 0;

        @Override
        public void run() {
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
                            status.put("state", TaskExecutionStates.EXECUTING.toString());
                            cr.updateStatus(updates.toString());

                            try {
                                String taskType = task.getType();
                                logger.debug("Task {} of type {} executing...", taskName, taskType);

                                // execute task
                                task.execute();

                                taskCount += 1;
                                logger.debug("Task {} of type {} completed. Executor total: {}", taskName, taskType, taskCount);

                                //update state to completed
                                status.put("state", TaskExecutionStates.COMPLETED.toString());
                                Map<String, Object> result = cr.updateStatus(updates.toString());

                                logger.debug("Updated resource: {}", result);

                            } catch (Exception e) {
                                logger.error("Task {} failed with exception {}", taskName, e);

                                status.put("state", TaskExecutionStates.FAILED.toString());
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
