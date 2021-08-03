import com.nirmata.workflow.WorkflowApp;
import com.nirmata.workflow.WorkflowAppBuilder;
import com.nirmata.workflow.WorkflowTask;

import java.util.concurrent.TimeUnit;

public class WorkflowAppBuilderTest {
    private static final int THREAD_POOL_SIZE = 3;
    public static void main (String[] args) {
        WorkflowTask wfTask = new WorkflowTask() {
            @Override
            public void execute() throws Exception {
                TimeUnit.SECONDS.sleep(5);
            }

            @Override
            public String getType() {
                return "testType";
            }
        };
        WorkflowApp workflowApp = WorkflowAppBuilder.builder().addTaskExecutor(wfTask, THREAD_POOL_SIZE).build();
        workflowApp.startApp();
    }
}
