package com.nirmata.workflow;

import com.google.common.base.Preconditions;
import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.task.TaskExecutor;
import com.nirmata.workflow.task.TaskWatcher;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WorkflowApp {
    private final String instanceName;
    private final String namespace;
    private final TimeUnit timeoutUnits;
    private final long timeout;
    private final CustomResourceDefinitionContext context;
    private final Map<String, TaskExecutor> executors;
    private static MixedOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> workflowTaskClient = null;

    public WorkflowApp(String instanceName, String namespace, TimeUnit timeoutUnits, long timeout, CustomResourceDefinitionContext context, Map<String, TaskExecutor> executors) {
        this.instanceName = Preconditions.checkNotNull(instanceName, "instanceName cannot be null");
        this.namespace = Preconditions.checkNotNull(namespace, "namespace cannot be null");
        this.timeoutUnits = Preconditions.checkNotNull(timeoutUnits, "timeoutUnits cannot be null");
        this.timeout = Preconditions.checkNotNull(timeout, "timeout cannot be null");
        this.context = Preconditions.checkNotNull(context, "namespace cannot be null");
        this.executors = Preconditions.checkNotNull(executors, "executors cannot be null");
    }

    public void startApp() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            //RawCustomResourceOperationsImpl api = client.customResource(context).inNamespace(namespace);
            workflowTaskClient = client.customResources(WorkflowTask.class);

            TaskWatcher watch = new TaskWatcher(workflowTaskClient, namespace, executors);
            watch.start();

            for (Map.Entry<String, TaskExecutor> e : executors.entrySet()) {
                e.getValue().start(workflowTaskClient);
            }

            timeoutUnits.sleep(timeout);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}