package com.nirmata.workflow.crd;

public class WorkflowTaskStatus {
    private String state;
    private String startTimeUTC;
    private String completionTimeUTC;
    private String executor;
    private String error;

    public enum ExecutionState {
        EXECUTING,
        COMPLETED,
        FAILED
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setState(ExecutionState state) {
        this.state = state.toString();
    }

    public String getStartTimeUTC() {
        return startTimeUTC;
    }

    public void setStartTimeUTC(String startTimeUTC) {
        this.startTimeUTC = startTimeUTC;
    }

    public String getCompletionTimeUTC() {
        return completionTimeUTC;
    }

    public void setCompletionTimeUTC(String completionTimeUTC) {
        this.completionTimeUTC = completionTimeUTC;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "WorkflowTaskStatus{state=" + state + "," +
            "startTimeUTC=" + startTimeUTC + "," +
            "completionTimeUTC=" + completionTimeUTC + "," +
            "executor=" + executor + "," +
            "error=" + error + "}";
    }
}
