package com.nirmata.workflow;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class WorkflowAppBuilder {
    private KubernetesClient client;
    private String instanceName;
    private String namespace = "default";

    private final Map<String, TaskExecutor> executors = new HashMap<>();

    public static WorkflowAppBuilder builder() {
        return new WorkflowAppBuilder();
    }

    public WorkflowAppBuilder withClient(KubernetesClient client) {
        this.client = Preconditions.checkNotNull(client, "client cannot be null");
        return this;
    }

    public WorkflowAppBuilder addTaskExecutor(Task task, int threadPoolSize) {
        String taskType = task.getType();
        executors.put(taskType, new TaskExecutor(task, threadPoolSize));
        return this;
    }

    public WorkflowAppBuilder addTaskExecutor(Task task, int threadPoolSize, long threadKeepAliveTime, TimeUnit unit) {
        String taskType = task.getType();
        executors.put(taskType, new TaskExecutor(task, threadPoolSize, threadKeepAliveTime, unit));
        return this;
    }

    public WorkflowAppBuilder addTaskExecutor(Task task, Executor executor) {
        String taskType = task.getType();
        executors.put(taskType, new TaskExecutor(task, executor));
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

    public WorkflowApp build() {
        return new WorkflowApp(client, instanceName, namespace, executors);
    }

    private WorkflowAppBuilder() {
        try {
            instanceName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            instanceName = "unknown";
        }
    }
}
