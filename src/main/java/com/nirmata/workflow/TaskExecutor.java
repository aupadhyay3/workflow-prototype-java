package com.nirmata.workflow;

import com.nirmata.workflow.WorkflowTask;
import java.util.Queue;

public class TaskExecutor {
    Queue<WorkflowTask> workQueue;

    public TaskExecutor(Queue<WorkflowTask> workQueue) {
        this.workQueue = workQueue;
    }

    public void run() {
        while (true) {
            WorkflowTask task = workQueue.poll();
            if (task != null) {
                task.execute();
            }
        }
    }
}
