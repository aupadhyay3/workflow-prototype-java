package com.nirmata.workflow;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class WorkflowApp {
    private final String NAMESPACE = "default";
    private final int NUM_EXECUTORS = 3;
    private final int TIMEOUT_MINUTES = 10;

    public void startApp() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1")
                    .withGroup("nirmata.com")
                    .withScope("Namespaced")
                    .withPlural("workflowtasks")
                    .build();
            RawCustomResourceOperationsImpl api = client.customResource(context).inNamespace(NAMESPACE);

            TaskWatch watch = new TaskWatch();
            Queue<String> wq = watch.startWatch(api);

            for (int i = 0; i < NUM_EXECUTORS; i++) {
                TaskExecutor executor = new TaskExecutor(wq, api, new TestTask());
                executor.start();
            }

            TimeUnit.MINUTES.sleep(TIMEOUT_MINUTES);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        WorkflowApp workflowTaskApp = new WorkflowApp();
        workflowTaskApp.startApp();
    }
}