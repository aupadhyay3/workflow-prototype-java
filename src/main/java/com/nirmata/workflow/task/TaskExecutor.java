package com.nirmata.workflow.task;

import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.crd.WorkflowTaskStatus;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    private final BlockingQueue<WorkflowTask> workQueue = new LinkedBlockingQueue<>();
    private final Task task;
    private final int threadPoolSize;
    private String podName;

    private NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>, Resource<WorkflowTask>> api;

    public TaskExecutor(Task task, int threadPoolSize) {
        this.task = task;
        this.threadPoolSize = threadPoolSize;
        try {
            podName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            podName = "unknown";
        }
    }

    public void start(NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> api) {
        this.api = api;
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
                        String taskName = queuedTask.getMetadata().getName();

                        if (queuedTask.getStatus() == null || queuedTask.getStatus().getExecutor() == null) {

                            WorkflowTaskStatus status;
                            if (queuedTask.getStatus() == null) {
                                status = new WorkflowTaskStatus();
                                queuedTask.setStatus(status);
                            } else {
                                status = queuedTask.getStatus();
                            }

                            //set executor field
                            status.setExecutor(executorName);

                            //update state to executing
                            status.setState(TaskExecutionState.EXECUTING);
                            api.patchStatus(queuedTask);

                            try {
                                String taskType = queuedTask.getSpec().getType();
                                logger.debug("Task {} of type {} executing...", taskName, taskType);

                                // execute task
                                task.execute(queuedTask);

                                taskCount += 1;
                                logger.debug("Task {} of type {} completed. Executor total: {}", taskName, taskType, taskCount);

                                //update state to completed
                                status.setState(TaskExecutionState.COMPLETED);
                                WorkflowTask result = api.patchStatus(queuedTask);

                                logger.debug("Updated resource: {}", result);

                            } catch (Exception e) {
                                logger.error("Task {} failed with exception {}", taskName, e);

                                status.setState(TaskExecutionState.FAILED);
                                api.patchStatus(queuedTask);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
