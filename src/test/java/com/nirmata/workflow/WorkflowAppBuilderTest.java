package com.nirmata.workflow;

import java.util.concurrent.TimeUnit;

import com.nirmata.workflow.task.Task;

public class WorkflowAppBuilderTest {
    private static final int THREAD_POOL_SIZE = 3;
    public static void main (String[] args) {
        Task wfTaskA = new Task() {
            @Override
            public void execute() throws Exception {
                TimeUnit.SECONDS.sleep(5);
            }

            @Override
            public String getType() {
                return "testTypeA";
            }
        };

        Task wfTaskB = new Task() {
            @Override
            public void execute() throws Exception {
                TimeUnit.SECONDS.sleep(5);
            }

            @Override
            public String getType() {
                return "testTypeB";
            }
        };

        //testOneTaskType(wfTaskA, THREAD_POOL_SIZE);
        testTwoTaskTypes(wfTaskA, wfTaskB, THREAD_POOL_SIZE);
    }

    /**
     * Test using a single task type and a specified number of executors.
     * Note: User must run a different script to schedule tasks.
     * @param taskType WorkflowTask object's type field which will be matched with its respective executor
     * @param executors Number of executors
     */
    public static void testOneTaskType (Task taskType, int executors) {
        WorkflowApp workflowApp = WorkflowAppBuilder.builder().addTaskExecutor(taskType, executors).build();
        workflowApp.startApp();
    }

    /**
     * Test using two task types and a specified number of executors.
     * Note: User must run a different script to schedule tasks.
     * @param taskType1, WorkflowTask object's type field which will be matched with its respective executor
     * @param taskType2, WorkflowTask object's type field which will be matched with its respective executor
     * @param executors Number of executors
     */
    public static void testTwoTaskTypes (Task taskType1, Task taskType2,  int executors) {
        WorkflowApp workflowApp = WorkflowAppBuilder.builder().addTaskExecutor(taskType1, THREAD_POOL_SIZE).
                addTaskExecutor(taskType2, THREAD_POOL_SIZE).build();
        workflowApp.startApp();
    }
}
