package com.nirmata.workflow.sample;

import com.nirmata.workflow.Task;
import com.nirmata.workflow.WorkflowApp;
import com.nirmata.workflow.WorkflowAppBuilder;
import com.nirmata.workflow.crd.WorkflowTask;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SampleApp {
    private static final int TASK_SLEEP_TIME_SECONDS = 5;
    private static final int THREAD_POOL_SIZE = 3;
    private static final int TIMEOUT_MINUTES = 20;

    private static final String NAMESPACE = "default";

    private static final Logger logger = LoggerFactory.getLogger(SampleApp.class);

    private static KubernetesClient client;

    private static void runWorkflowApp() throws Exception {
        client = new DefaultKubernetesClient();
        logger.debug("Kubernetes client started");

        Task sampleTask = new Task() {
            @Override
            public void execute(WorkflowTask resource) throws Exception {
                TimeUnit.SECONDS.sleep(TASK_SLEEP_TIME_SECONDS);
            }
    
            @Override
            public String getType() {
                return "sample";
            }
        };

        WorkflowApp workflowApp = WorkflowAppBuilder.builder()
            .withClient(client)
            .withNamespace(NAMESPACE)
            .addTaskExecutor(sampleTask, THREAD_POOL_SIZE)
            .build();
        workflowApp.start();
        logger.debug("WorkflowApp started");

        TimeUnit.MINUTES.sleep(TIMEOUT_MINUTES);
        logger.info("Timed out");
    }

    public static void main(String[] args) {
        try {
            runWorkflowApp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
