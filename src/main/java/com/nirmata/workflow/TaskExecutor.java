package com.nirmata.workflow;

import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.crd.WorkflowTaskStatus;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class TaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    private final Task task;
    private final Executor executor;

    private int taskCount = 0;
    private String podName;

    private NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>, Resource<WorkflowTask>> api;

    public TaskExecutor(Task task, Executor executor) {
        this.task = task;
        this.executor = executor;
        try {
            podName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            podName = "unknown";
        }
    }

    public TaskExecutor(Task task, int threadPoolSize, long threadKeepAliveTime, TimeUnit unit) {
        this(task, new ThreadPoolExecutor(threadPoolSize, threadPoolSize, threadKeepAliveTime, unit, new LinkedBlockingQueue<>()));
    }

    public TaskExecutor(Task task, int threadPoolSize) {
        this(task, threadPoolSize, 60L, TimeUnit.SECONDS);
    }

    public void initialize(NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> api) {
        this.api = api;
    }

    public void execute(WorkflowTask taskResource) {
        executor.execute(new TaskExecution(taskResource));
    }

    private class TaskExecution implements Runnable {
        private final String executorName = podName + "-" + Thread.currentThread().getId();
        private WorkflowTask taskResource;

        public TaskExecution(WorkflowTask taskResource) {
            this.taskResource = taskResource;
        }

        @Override
        public void run() {
            try {
                if (taskResource != null) {
                    String taskName = taskResource.getMetadata().getName();

                    WorkflowTaskStatus status;
                    if (taskResource.getStatus() == null) {
                        status = new WorkflowTaskStatus();
                        taskResource.setStatus(status);
                    } else {
                        status = taskResource.getStatus();
                    }

                    if (status.getExecutor() == null) {

                        //set executor field
                        status.setExecutor(executorName);

                        //update state to executing
                        status.setState(WorkflowTaskStatus.ExecutionState.EXECUTING);
                        try {
                            taskResource = api.patchStatus(taskResource);
                        } catch (KubernetesClientException e) {
                            if (e.getStatus().getCode() == 409) {
                                //logger.debug("http error 409");
                                return;
                            } else {
                                e.printStackTrace();
                            }
                        }

                        String taskType = taskResource.getSpec().getType();
                        logger.debug("Task {} of type {} executing...", taskName, taskType);

                        try {
                            // execute task
                            task.execute(taskResource);

                        } catch (Exception e) {
                            logger.error("Task {} failed with exception {}", taskName, e);

                            status.setState(WorkflowTaskStatus.ExecutionState.FAILED);
                            taskResource.setStatus(status);
                            taskResource = api.patchStatus(taskResource);
                        }

                        taskCount += 1;
                        logger.debug("Task {} of type {} completed. Executor total: {}", taskName, taskType, taskCount);

                        //update state to completed
                        status.setState(WorkflowTaskStatus.ExecutionState.COMPLETED);
                        taskResource.setStatus(status);
                        taskResource = api.patchStatus(taskResource);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
