/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.WorkItemEvent;
import com.evolveum.midpoint.notifications.api.events.WorkflowEvent;
import com.evolveum.midpoint.notifications.api.events.WorkflowEventCreator;
import com.evolveum.midpoint.notifications.api.events.WorkflowProcessEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.ProcessListener;
import com.evolveum.midpoint.wf.api.WorkItemListener;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Listener that accepts events generated by workflow module. These events are related to processes and work items.
 *
 * @author mederly
 */
@Component
public class WorkflowListener implements ProcessListener, WorkItemListener {

    private static final Trace LOGGER = TraceManager.getTrace(WorkflowListener.class);

    //private static final String DOT_CLASS = WorkflowListener.class.getName() + ".";

    @Autowired
    private NotificationManager notificationManager;

    // WorkflowManager is not required, because e.g. within model-test and model-intest we have no workflows.
    // However, during normal operation, it is expected to be available.

    @Autowired(required = false)
    private WorkflowManager workflowManager;

    @PostConstruct
    public void init() {
        if (workflowManager != null) {
            workflowManager.registerProcessListener(this);
            workflowManager.registerWorkItemListener(this);
        } else {
            LOGGER.warn("WorkflowManager not present, notifications for workflows will not be enabled.");
        }
    }

    @Override
    public void onProcessInstanceStart(PrismObject<? extends ProcessInstanceState> instanceState, OperationResult result) {
        WorkflowEventCreator workflowEventCreator = notificationManager.getWorkflowEventCreator(instanceState);
        WorkflowProcessEvent event = workflowEventCreator.createWorkflowProcessStartEvent(instanceState, result);
        processEvent(event, result);
    }

    @Override
    public void onProcessInstanceEnd(PrismObject<? extends ProcessInstanceState> instanceState, OperationResult result) {
        WorkflowEventCreator workflowEventCreator = notificationManager.getWorkflowEventCreator(instanceState);
        WorkflowProcessEvent event = workflowEventCreator.createWorkflowProcessEndEvent(instanceState, result);
        processEvent(event, result);
    }

    @Override
    public void onWorkItemCreation(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState) {
        WorkflowEventCreator workflowEventCreator = notificationManager.getWorkflowEventCreator(instanceState);
        WorkItemEvent event = workflowEventCreator.createWorkItemCreateEvent(workItemName, assigneeOid, instanceState);
        processEvent(event);
    }

    @Override
    public void onWorkItemCompletion(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState, String decision) {
        WorkflowEventCreator workflowEventCreator = notificationManager.getWorkflowEventCreator(instanceState);
        WorkItemEvent event = workflowEventCreator.createWorkItemCompleteEvent(workItemName, assigneeOid, instanceState, decision);
        processEvent(event);
    }

    private void processEvent(WorkflowEvent event, OperationResult result) {
        try {
            notificationManager.processEvent(event);
        } catch (RuntimeException e) {
            result.recordFatalError("An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
            LoggingUtils.logException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
        }

        // todo work correctly with operationResult (in whole notification module)
        if (result.isUnknown()) {
            result.computeStatus();
        }
        result.recordSuccessIfUnknown();
    }

    private void processEvent(WorkflowEvent event) {
        try {
            notificationManager.processEvent(event);
        } catch (RuntimeException e) {
            LoggingUtils.logException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
        }
    }
}