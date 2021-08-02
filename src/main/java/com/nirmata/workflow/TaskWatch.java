package com.nirmata.workflow;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TaskWatch {
    private static final Logger logger = LoggerFactory.getLogger(TaskWatch.class);
    private BlockingQueue<String> workQueue;

    public Queue<String> startWatch(RawCustomResourceOperationsImpl api) {
        workQueue = new LinkedBlockingQueue<>();

        try {
            api.watch(new Watcher<String>() {
                @Override
                public void eventReceived(Action action, String resource) {
                    if (action == Action.ADDED) {
                        workQueue.add(resource);
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
            logger.info("Watch Started");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return workQueue;
    }
}
