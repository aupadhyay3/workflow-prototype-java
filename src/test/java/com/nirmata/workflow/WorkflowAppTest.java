package com.nirmata.workflow;

import com.nirmata.workflow.crd.WorkflowTask;
import com.nirmata.workflow.crd.WorkflowTaskSpec;
import com.nirmata.workflow.task.Task;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorkflowAppTest {
    private static final int NUM_TASKS = 10;
    private static final int TASK_SLEEP_TIME_SECONDS = 5;
    private static final int THREAD_POOL_SIZE = 2;
    private static final int NUM_INSTANCES = 2;
    private static final int TEST_TIMEOUT_MINUTES = 5;

    private static final String NAMESPACE = "default";

    private static final Logger logger = LoggerFactory.getLogger(WorkflowAppTest.class);

    private static KubernetesClient client;

    @BeforeClass
    public static void setup() throws Exception {
        client = new DefaultKubernetesClient();
        CustomResourceDefinition crd = client.apiextensions().v1().customResourceDefinitions()
            .load(new FileInputStream(Paths.get("").toAbsolutePath().toString() + 
                "/src/main/java/com/nirmata/workflow/crd/workflowtask-crd.yaml"))
            .get();
        client.apiextensions().v1().customResourceDefinitions().createOrReplace(crd);
    }

    @AfterClass
    public static void teardown() {
        client.customResources(WorkflowTask.class).inNamespace(NAMESPACE).delete();
        client.close();
    }

    /**
     * Test using a single task type and a specified number of executors.
     */
    @Test
    public void testOneTaskType() throws Exception {
        Task wfTask = createTestTask("testType", TASK_SLEEP_TIME_SECONDS);

        WorkflowApp workflowApp = WorkflowAppBuilder.builder()
            .withClient(client)
            .withNamespace(NAMESPACE)
            .addTaskExecutor(wfTask, THREAD_POOL_SIZE)
            .build();
        workflowApp.start();

        scheduleTasks(wfTask, NUM_TASKS);

        TimeUnit.MINUTES.sleep(TEST_TIMEOUT_MINUTES);
    }

    /**
     * Test using two task types and a specified number of executors.
     */
    @Test
    public void testTwoTaskTypes() throws Exception {
        Task wfTaskA = createTestTask("testTypeA", TASK_SLEEP_TIME_SECONDS);
        Task wfTaskB = createTestTask("testTypeB", TASK_SLEEP_TIME_SECONDS);

        WorkflowApp workflowApp = WorkflowAppBuilder.builder()
                .withClient(client)
                .withNamespace(NAMESPACE)
                .addTaskExecutor(wfTaskA, THREAD_POOL_SIZE)
                .addTaskExecutor(wfTaskB, THREAD_POOL_SIZE)
                .build();
        workflowApp.start();

        scheduleTasks(wfTaskA, NUM_TASKS);
        scheduleTasks(wfTaskB, NUM_TASKS);

        TimeUnit.MINUTES.sleep(TEST_TIMEOUT_MINUTES);
    }

    /**
     * Test using one task type and a specified number of WorkflowApp instances,
     * each with a specified number of executors.
     */
    @Test
    public void testMultipleInstances() throws Exception {
        Task wfTask = createTestTask("testType", TASK_SLEEP_TIME_SECONDS);

        for (int i = 0; i < NUM_INSTANCES; i++) {
            WorkflowApp workflowApp = WorkflowAppBuilder.builder()
                .withClient(client)
                .withNamespace(NAMESPACE)
                .addTaskExecutor(wfTask, THREAD_POOL_SIZE)
                .build();
            workflowApp.start();
        }

        scheduleTasks(wfTask, NUM_TASKS);

        TimeUnit.MINUTES.sleep(TEST_TIMEOUT_MINUTES);
    }

    private Task createTestTask(String type, int sleepTimeSeconds) {
        Task testTask = new Task() {
            @Override
            public void execute(WorkflowTask resource) throws Exception {
                TimeUnit.SECONDS.sleep(sleepTimeSeconds);
            }
    
            @Override
            public String getType() {
                return type;
            }
        };
        return testTask;
    }

    private void scheduleTasks(Task task, int quantity) {
        UUID batchID = UUID.randomUUID();

        for (int i = 0; i < quantity; i++) {
            String resourceName = "test-" + batchID + "-" + i;

            WorkflowTask resource = new WorkflowTask();
            ObjectMeta metadata = new ObjectMeta();
            metadata.setName(resourceName);
            resource.setMetadata(metadata);

            WorkflowTaskSpec spec = new WorkflowTaskSpec();
            spec.setType(task.getType());
            resource.setSpec(spec);

            client.customResources(WorkflowTask.class)
                .inNamespace(NAMESPACE)
                .create(resource);
            logger.debug("Scheduled task {}", resourceName);
        }
    }
}
