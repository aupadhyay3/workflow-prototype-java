package com.nirmata.workflow;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowTaskApp {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowTaskApp.class);

    private void start() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1")
                    .withGroup("nirmata.com")
                    .withScope("Namespaced")
                    .withPlural("workflowtasks")
                    .build();
            RawCustomResourceOperationsImpl cr = client.customResource(context).inNamespace("default");

            BlockingQueue<String> dispatch = new LinkedBlockingQueue<>();

            cr.watch(new Watcher<String>() {
                @Override
                public void eventReceived(Action action, String resource) {
                    try {
                        if (action == Action.ADDED) {
                            dispatch.put(resource);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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

            TimeUnit.MINUTES.sleep(10);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        WorkflowTaskApp workflowTaskApp = new WorkflowTaskApp();
        workflowTaskApp.start();
    }
}
