package com.nirmata.workflow;

import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class WorkflowAppBuilder {
    private String instanceName;
    private String namespace = "default";
    private TimeUnit timeoutUnits = TimeUnit.MINUTES;
    private long timeout = 10;
    private CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
        .withVersion("v1")
        .withGroup("nirmata.com")
        .withScope("Namespaced")
        .withPlural("workflowtasks")
        .build();

    private final Map<String, TaskExecutor> executors = new HashMap<>();

    public static WorkflowAppBuilder builder() {
        return new WorkflowAppBuilder();
    }

    public WorkflowAppBuilder addTaskExecutor(WorkflowTask task, int threadPoolSize) {
        String taskType = task.getType();
        TaskExecutor taskExecutor = new TaskExecutor(task, threadPoolSize);
        executors.put(taskType, taskExecutor);
        return this;
    }

    public WorkflowAppBuilder withInstanceName(String instanceName) {
        this.instanceName = Preconditions.checkNotNull(instanceName, "instanceName cannot be null");
        return this;
    }

    public WorkflowAppBuilder withNamespace(String namespace) {
        this.namespace = Preconditions.checkNotNull(namespace, "namespace cannot be null");
        return this;
    }

    public WorkflowAppBuilder withTimeout(TimeUnit timeoutUnits, long timeout) {
        this.timeoutUnits = Preconditions.checkNotNull(timeoutUnits, "timeoutUnits cannot be null");
        this.timeout = Preconditions.checkNotNull(timeout, "timeout cannot be null");
        return this;
    }

    public WorkflowAppBuilder withContext(CustomResourceDefinitionContext context) {
        this.context = Preconditions.checkNotNull(context, "namespace cannot be null");
        return this;
    }

    public WorkflowApp build() {
        return new WorkflowApp(instanceName, namespace, timeoutUnits, timeout, context, executors);
    }

    private WorkflowAppBuilder() {
        try {
            instanceName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            instanceName = "unknown";
        }
    }

    public static void main (String[] args) {
        WorkflowTask wfTask = new WorkflowTask() {
            @Override
            public void execute() throws Exception {
                TimeUnit.SECONDS.sleep(5);
            }

            @Override
            public String getType() {
                return "testType";
            }
        };
        WorkflowApp workflowApp = new WorkflowAppBuilder().addTaskExecutor(wfTask, 3).build();
        workflowApp.startApp();
    }
}
