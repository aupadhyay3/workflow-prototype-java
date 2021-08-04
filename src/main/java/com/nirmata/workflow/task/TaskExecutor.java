package com.nirmata.workflow.task;

import com.nirmata.workflow.WorkflowApp;
import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.crd.WorkflowTaskStatus;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
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

    private final BlockingQueue<WorkflowTask> workQueue = new LinkedBlockingQueue<>();
    private final Task task;
    private final int threadPoolSize;
    private String podName;

    private static MixedOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> workflowTaskClient = null;

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

    public void start(MixedOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> workflowTaskClient) {
        this.workflowTaskClient = workflowTaskClient;
        for (int i = 0; i < threadPoolSize; i++) {
            Thread t = new ExecutorThread();
            t.start();
        }
    }

    public void addTask(WorkflowTask workflowTask) {
        workQueue.add(workflowTask);
    }

    private class ExecutorThread extends Thread {
        @Override
        public void run() {
            String executorName = podName + "-" + getId();
            int taskCount = 0;

            while (true) {
                try {
                    WorkflowTask queuedTask = workQueue.poll();

                    if (queuedTask != null) {
                        String taskName = queuedTask.getCRDName();
                        Resource cr = workflowTaskClient.withName(taskName);
                        //RawCustomResourceOperationsImpl cr = api.withName(taskName);

                        //System.out.println("status" + queuedTask.getStatus());
                        //System.out.println("executor" + queuedTask.getStatus().getExecutor());
                        if (queuedTask.getStatus() == null || queuedTask.getStatus().getExecutor() == null) {
                            //JSONObject updates = new JSONObject();

                            if (queuedTask.getStatus() == null) {
                                queuedTask.setStatus(new WorkflowTaskStatus());
                            }
                            WorkflowTaskStatus status = queuedTask.getStatus();
                            //updates.put("status", status);

                            //set executor field
                            status.setExecutor(executorName);

                            //update state to executing
                            status.setState(TaskExecutionState.EXECUTING);
                            //cr.updateStatus(updates.toString());
                            cr.replaceStatus(status);

                            try {
                                String taskType = queuedTask.getSpec().getType();
                                logger.debug("Task {} of type {} executing...", taskName, taskType);

                                // execute task
                                task.execute();

                                taskCount += 1;
                                logger.debug("Task {} of type {} completed. Executor total: {}", taskName, taskType, taskCount);

                                //update state to completed
                                status.setState(TaskExecutionState.COMPLETED);
                                cr.replaceStatus(status);
                                //Map<String, Object> result = cr.updateStatus(updates.toString());

                                logger.debug("Updated resource: {}", cr);

                            } catch (Exception e) {
                                logger.error("Task {} failed with exception {}", taskName, e);

                                status.setState(TaskExecutionState.FAILED);
                                cr.replaceStatus(status);
                                //cr.updateStatus(updates.toString());
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
