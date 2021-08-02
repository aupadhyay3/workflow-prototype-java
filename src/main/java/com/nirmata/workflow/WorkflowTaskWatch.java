package com.nirmata.workflow;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorkflowTaskWatch {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowTaskWatch.class);
    private final int EXEC_TIME = 5;
    private int taskCount = 0;

    public void startWatch(String ns) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1")
                    .withGroup("nirmata.com")
                    .withScope("Namespaced")
                    .withPlural("workflowtasks")
                    .build();
            RawCustomResourceOperationsImpl cr = client.customResource(context).inNamespace(ns);
            processEvent(cr);
            logger.info("Watch Started");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processEvent(RawCustomResourceOperationsImpl cr) {
        try {
            cr.watch(new Watcher<String>() {
                @Override
                public void eventReceived(Action action, String resource) {
                    try {
                        JSONObject json = new JSONObject(resource);
                        String taskName = json.getJSONObject("metadata").getString("name");

                        if (action == Action.ADDED) {
                            logger.info("Added WorkflowTask {}", taskName);
                            handleEvent(cr, taskName, "testType");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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

            logger.info("hi");

            TimeUnit.MINUTES.sleep(10);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleEvent(RawCustomResourceOperationsImpl cr, String taskName, String executorType) {
        //check whether task's executor field is set
        Map<String, Object> taskMap = cr.withName(taskName).get();

        if (taskMap.get("status") == null || !taskMap.toString().contains("executor")) {
            logger.info("no executor");

            //check whether task type matches executor type
            String typeString = taskMap.get("spec").toString();
            String taskType = typeString.substring(typeString.indexOf("=") + 1, typeString.indexOf("}"));

            if (taskType.equals(executorType)) {
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
                    result = cr.withName(taskName).updateStatus(status.toString());
                    logger.info("Task {} executing...", taskName);

                    //sleep for execution time period
                    TimeUnit.SECONDS.sleep(EXEC_TIME);

                    taskCount++;
                    logger.info("Task {} completed. Executor total: {}", taskName, taskCount);
                    //update state to completed
                    statusFields.put("state", "COMPLETED");
                    status.put("status", statusFields);
                    result = cr.withName(taskName).updateStatus(status.toString());
                    logger.info("{}", result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            logger.info("Sending task to another executor");
        }

    }

}
