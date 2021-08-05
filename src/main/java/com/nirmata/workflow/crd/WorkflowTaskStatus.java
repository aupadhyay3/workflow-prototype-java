package com.nirmata.workflow.crd;

public class WorkflowTaskStatus {
    private ExecutionState state;
    private String startTimeUTC;
    private String completionTimeUTC;
    private String executor;
    private String error;

    public enum ExecutionState {
        EXECUTING,
        COMPLETED,
        FAILED
    }

    public ExecutionState getState() {
        return state;
    }

    public void setState(ExecutionState state) {
        this.state = state;
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
