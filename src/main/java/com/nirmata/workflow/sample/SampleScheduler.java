package com.nirmata.workflow.sample;

import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.crd.WorkflowTaskSpec;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SampleScheduler {
    private static final int NUM_TASKS = 20;

    private static final String NAMESPACE = "default";

    private static final Logger logger = LoggerFactory.getLogger(SampleScheduler.class);

    private static KubernetesClient client;

    private static void scheduleTasks() {
        client = new DefaultKubernetesClient();
        UUID batchID = UUID.randomUUID();

        for (int i = 1; i <= NUM_TASKS; i++) {
            String resourceName = "test-" + batchID + "-" + i;

            WorkflowTask resource = new WorkflowTask();
            ObjectMeta metadata = new ObjectMeta();
            metadata.setName(resourceName);
            resource.setMetadata(metadata);

            WorkflowTaskSpec spec = new WorkflowTaskSpec();
            spec.setType("sample");
            resource.setSpec(spec);

            client.customResources(WorkflowTask.class)
                .inNamespace(NAMESPACE)
                .create(resource);

            if (i % 20 == 0) {
                logger.debug("Scheduled {} tasks", i);
            }
        }

        logger.debug("Completed scheduling {} tasks", NUM_TASKS);
    }

    public static void main(String[] args) {
        scheduleTasks();
    }
}
