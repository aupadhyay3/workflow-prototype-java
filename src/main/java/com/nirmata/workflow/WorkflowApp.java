package com.nirmata.workflow;

import com.google.common.base.Preconditions;
import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.task.TaskExecutor;
import com.nirmata.workflow.task.TaskWatcher;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.Map;

public class WorkflowApp {
    private final KubernetesClient client;
    private final String instanceName;
    private final String namespace;
    private final Map<String, TaskExecutor> executors;

    public WorkflowApp(KubernetesClient client, String instanceName, String namespace, Map<String, TaskExecutor> executors) {
        this.client = Preconditions.checkNotNull(client, "client cannot be null");
        this.instanceName = Preconditions.checkNotNull(instanceName, "instanceName cannot be null");
        this.namespace = Preconditions.checkNotNull(namespace, "namespace cannot be null");
        this.executors = Preconditions.checkNotNull(executors, "executors cannot be null");
    }

    public void start() {
        try {
            NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>, 
                Resource<WorkflowTask>> api = client.customResources(WorkflowTask.class).inNamespace(namespace);

            TaskWatcher watch = new TaskWatcher(api, executors);
            watch.start();

            for (Map.Entry<String, TaskExecutor> e : executors.entrySet()) {
                e.getValue().start(api);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}