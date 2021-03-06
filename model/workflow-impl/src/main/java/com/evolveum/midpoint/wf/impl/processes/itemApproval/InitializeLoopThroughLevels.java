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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.ArrayList;

public class InitializeLoopThroughLevels implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughLevels.class);

    public void execute(DelegateExecution execution) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Executing the delegate; execution = " + execution);
        }

//        ApprovalRequest itemToApprove = (ApprovalRequest) execution.getVariable(ProcessVariableNames.APPROVAL_REQUEST);
//        Validate.notNull(itemToApprove, "itemToApprove is null");

        execution.setVariableLocal(ProcessVariableNames.LOOP_LEVELS_STOP, Boolean.FALSE);
        execution.setVariableLocal(ProcessVariableNames.ALL_DECISIONS, new ArrayList<Decision>());
    }

}
