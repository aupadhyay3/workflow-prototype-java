import com.fasterxml.jackson.databind.util.JSONPObject;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.json.JSONObject;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class WorfklowTaskApp {

    public void watch () {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            SharedInformerFactory informerFactory = client.informers();
            CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1")
                    .withGroup("nirmata.com")
                    .withScope("Namespaced")
                    .withPlural("workflowtasks")
                    .build();


            SharedIndexInformer<GenericKubernetesResource> informer = informerFactory.sharedIndexInformerForCustomResource(context, 60 * 1000L);
            informer.addEventHandler(new ResourceEventHandler<GenericKubernetesResource>() {
                @Override
                public void onAdd(GenericKubernetesResource genericKubernetesResource) {
                    System.out.printf("ADD %s\n", genericKubernetesResource.getMetadata().getName());
                }

                @Override
                public void onUpdate(GenericKubernetesResource genericKubernetesResource, GenericKubernetesResource t1) {
                    System.out.printf("UPDATE %s\n", genericKubernetesResource.getMetadata().getName());
                    executeTask(genericKubernetesResource);
                }

                @Override
                public void onDelete(GenericKubernetesResource genericKubernetesResource, boolean b) {
                    System.out.printf("DELETE %s\n", genericKubernetesResource.getMetadata().getName());
                }
            });

            informerFactory.startAllRegisteredInformers();

            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void executeTask(GenericKubernetesResource genericKubernetesResource) {

    }

    public static void main(String[] args) {
        WorfklowTaskApp worfklowTaskApp = new WorfklowTaskApp();
        worfklowTaskApp.watch();
    }
}
