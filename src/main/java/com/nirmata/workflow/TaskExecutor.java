package com.nirmata.workflow;

import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.Queue;

public class TaskExecutor extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    private final Queue<String> workQueue;
    private final RawCustomResourceOperationsImpl api;
    private final WorkflowTask task;

    public TaskExecutor(Queue<String> workQueue, RawCustomResourceOperationsImpl api, WorkflowTask task) {
        this.workQueue = workQueue;
        this.api = api;
        this.task = task;
    }

    public void run() {
        while (true) {
            try {
                String resource = workQueue.poll();

                if (resource != null) {
                    JSONObject json = new JSONObject(resource);
                    String taskName = json.getJSONObject("metadata").getString("name");

                    Map<String, Object> taskMap = api.withName(taskName).get();

                    if (taskMap.get("status") == null || !taskMap.toString().contains("executor")) {
                        //check whether task type matches executor type
                        String typeString = taskMap.get("spec").toString();
                        String taskType = typeString.substring(typeString.indexOf("=") + 1, typeString.indexOf("}"));
            
                        if (taskType.equals(task.getType())) {
                            logger.info("task and executor type match");
            
                            //set executor field using podname and an id
                            String podName = System.getenv("HOSTNAME");
                            String executorField = podName + UUID.randomUUID();
                            JSONObject statusFields = new JSONObject().put("executor", executorField);
                            JSONObject status = new JSONObject().put("status", statusFields);
                            Map<String, Object> result = null;
                            try {
                                //update state to executing
                                statusFields.put("state", "EXECUTING");
                                status.put("status", statusFields);
                                result = api.withName(taskName).updateStatus(status.toString());
                                logger.info("Task {} executing...", taskName);
            
                                task.execute();

                                //update state to completed
                                statusFields.put("state", "COMPLETED");
                                status.put("status", statusFields);
                                result = api.withName(taskName).updateStatus(status.toString());
                                logger.info("{}", result);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        workQueue.add(resource);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getStackTrace().toString());
            }
        }
    }
}
