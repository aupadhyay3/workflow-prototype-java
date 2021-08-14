# workflow-prototype-java #

## About ##

A Java prototype for Kubernetes-native Nirmata workflows. [WorkflowApp](src/main/com/nirmata/workflow/WorkflowApp.java) watches [WorkflowTask Kubernetes custom resources](src/main/java/com/nirmata/workflow/crd/workflowtask-crd.yaml) and executes user-defined Java tasks upon resource creation. Tasks of different types may be assigned individually to different executors and run in parallel.

A Golang implementation can be found [here](https://github.com/jacob-yim/workflow-prototype). 

## Usage ##

To create a new WorkflowApp, use [the builder](src/main/com/nirmata/workflow/WorkflowAppBuilder.java):

    WorkflowApp workflowApp = WorkflowAppBuilder.builder();

**Required:** A fabric8 KubernetesClient and a namespace string must be specified using the builder.

    WorkflowApp workflowApp = WorkflowAppBuilder.builder()
        .withClient(client)
        .withNamespace(namespace);

Finally, add tasks and executors. WorkflowApp.addTaskExecutor() takes in two parameters: a Task implementing the [Task interface](src/main/com/nirmata/workflow/Task.java) and an integer thread pool size. Using this method, tasks are executed by a ThreadPoolExecutor that terminates threads that have been idle for 60 seconds.

    WorkflowApp workflowApp = WorkflowAppBuilder.builder()
        .withClient(client)
        .withNamespace(namespace)
        .addTaskExecutor(task, threadPoolSize)
        .build();

The thread timeout may also be specified:

    WorkflowApp workflowApp = WorkflowAppBuilder.builder()
        .withClient(client)
        .withNamespace(namespace)
        .addTaskExecutor(task, threadPoolSize, threadKeepAliveTime, timeUnit)
        .build();

Any existing Executor can also be passed in instead:

    WorkflowApp workflowApp = WorkflowAppBuilder.builder()
        .withClient(client)
        .withNamespace(namespace)
        .addTaskExecutor(task, executor)
        .build();

Finally, start the WorkflowApp:

    workflowApp.start();

The Task interface contains two methods: execute() and getType(). execute() takes in a [WorkflowTask POJO](src/main/java/com/nirmata/workflow/crd/WorkflowTask.java) and performs the task execution, possibly throwing an Exception. getType() returns the type of the task as a string.