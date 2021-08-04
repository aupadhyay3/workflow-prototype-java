package com.nirmata.workflow.task;

import com.nirmata.workflow.crd.WorkflowTask;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class TaskWatcher {
    private static final Logger logger = LoggerFactory.getLogger(TaskWatcher.class);
    private final Map<String, TaskExecutor> executors;
    private static MixedOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> workflowTaskClient;
    private String namespace;

    public TaskWatcher(MixedOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>,
            Resource<WorkflowTask>> workflowTaskClient, String namespace, Map<String, TaskExecutor> executors) {
        this.workflowTaskClient = workflowTaskClient;
        this.namespace = namespace;
        this.executors = executors;
    }

    public void start() {
        workflowTaskClient.inNamespace(namespace).watch(new Watcher<WorkflowTask>() {
            @Override
            public void eventReceived(Action action, WorkflowTask workflowTask) {
                if (action == Action.ADDED) {
                    logger.info("task added");
                    String taskType = workflowTask.getSpec().getType();
                    if (executors.containsKey(taskType)) {
                        TaskExecutor executor = executors.get(taskType);
                        executor.addTask(workflowTask);
                    }
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
    }
}
