package com.nirmata.workflow.task;

import com.nirmata.workflow.crd.WorkflowTask;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TaskWatcher {
    private static final Logger logger = LoggerFactory.getLogger(TaskWatcher.class);

    private NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>, 
        Resource<WorkflowTask>> api;
    private Map<String, TaskExecutor> executors;

    public TaskWatcher(NonNamespaceOperation<WorkflowTask, KubernetesResourceList<WorkflowTask>, 
            Resource<WorkflowTask>> api, Map<String, TaskExecutor> executors) {
        this.api = api;
        this.executors = executors;
    }

    public void start() {
        api.watch(new Watcher<WorkflowTask>() {
            @Override
            public void eventReceived(Action action, WorkflowTask workflowTask) {
                if (action == Action.ADDED) {
                    //logger.info("task added");
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
