package com.nirmata.workflow;

import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Map;
import java.util.UUID;

public class TaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    private final WorkflowTask task;
    private final int threadPoolSize;
    private final BlockingQueue<JSONObject> workQueue = new LinkedBlockingQueue<>();

    private RawCustomResourceOperationsImpl api;

    public TaskExecutor(WorkflowTask task, int threadPoolSize) {
        this.task = task;
        this.threadPoolSize = threadPoolSize;
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
        private String threadID = "TODO";
        private int taskCount = 0;

        @Override
        public void run() {
            while (true) {
                try {
                    JSONObject json = workQueue.poll();

                    if (json != null) {
                        String taskName = json.getJSONObject("metadata").getString("name");

                        Map<String, Object> taskMap = api.withName(taskName).get();

                        if (taskMap.get("status") == null || !taskMap.toString().contains("executor")) {
                            //check whether task type matches executor type
                            String typeString = taskMap.get("spec").toString();
                            String taskType = typeString.substring(typeString.indexOf("=") + 1, typeString.indexOf("}"));
                
                            if (taskType.equals(task.getType())) {
                                //set executor field using podname and an id
                                String podName = InetAddress.getLocalHost().getHostName();
                                String executorField = podName + UUID.randomUUID();
                                JSONObject statusFields = new JSONObject().put("executor", executorField);
                                JSONObject status = new JSONObject().put("status", statusFields);
                                Map<String, Object> result = null;

                                //update state to executing
                                statusFields.put("state", "EXECUTING");
                                status.put("status", statusFields);
                                result = api.withName(taskName).updateStatus(status.toString());
                                logger.info("Task {} executing...", taskName);
            
                                task.execute();

                                //update state to completed
                                statusFields.put("state", "COMPLETED");
                                status.put("status", statusFields);
                                taskCount += 1;
                                logger.info("Task {} completed...Executor total: {}", taskName, taskCount);
                                result = api.withName(taskName).updateStatus(status.toString());
                                logger.info("Updated resource: {}", result);
                            }
                        } else {
                            workQueue.add(json);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getStackTrace().toString());
                }
            }
        }
    }
}
