package com.nirmata.workflow;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowTaskApp {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowTaskApp.class);

    public void startApp(String ns) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1")
                    .withGroup("nirmata.com")
                    .withScope("Namespaced")
                    .withPlural("workflowtasks")
                    .build();

            client.customResource(context).watch(ns, new Watcher<String>() {
                @Override
                public void eventReceived(Action action, String resource) {
                    try {
                        JSONObject json = new JSONObject(resource);
                        String taskName = json.getJSONObject("metadata").getString("name");

                        if (action == Action.ADDED) {
                            logger.info("Added WorkflowTask " + taskName);
                        }

                    } catch (JSONException e) {
                        logger.error("Failed to parse object");
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

            logger.info("hi");

            TimeUnit.MINUTES.sleep(10);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void executeTask(GenericKubernetesResource genericKubernetesResource) {

    }

    public static void main(String[] args) {
        WorkflowTaskApp workflowTaskApp = new WorkflowTaskApp();
        workflowTaskApp.startApp("default");
    }
}
