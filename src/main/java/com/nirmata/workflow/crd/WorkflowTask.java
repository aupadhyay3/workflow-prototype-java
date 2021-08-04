package com.nirmata.workflow.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("nirmata.com")
public class WorkflowTask extends CustomResource<WorkflowTaskSpec, WorkflowTaskStatus> implements Namespaced {
    
}
