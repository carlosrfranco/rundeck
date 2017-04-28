package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContext;
import com.dtolabs.rundeck.core.dispatcher.MultiDataContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.rules.*;

import java.util.Set;
import java.util.function.Function;

/**
 * operation for running a step
 */
class EngineWorkflowStepOperation implements WorkflowSystem.Operation<MultiDataContext<String,DataContext>,EngineWorkflowStepOperationCompleted> {
    int stepNum;
    String label;
    Set<Condition> startTriggerConditions;
    Set<Condition> skipTriggerConditions;
    private Function<MultiDataContext<String,DataContext>,BaseWorkflowExecutor.StepResultCapture> callable;
    private StateObj startTriggerState;
    private StateObj skipTriggerState;
    private boolean didRun = false;

    EngineWorkflowStepOperation(
            final int stepNum,
            final String label,
            final Function<MultiDataContext<String,DataContext>,BaseWorkflowExecutor.StepResultCapture> callable,
            final StateObj startTriggerState,
            final StateObj skipTriggerState,
            final Set<Condition> startTriggerConditions,
            final Set<Condition> skipTriggerConditions
    )
    {
        this.stepNum = stepNum;
        this.label = label;
        this.callable = callable;
        this.startTriggerState = startTriggerState;
        this.startTriggerConditions = startTriggerConditions;
        this.skipTriggerConditions = skipTriggerConditions;
        this.skipTriggerState = skipTriggerState;
    }

    @Override
    public boolean shouldRun(final StateObj state) {
        return state.hasState(startTriggerState);
    }

    @Override
    public boolean shouldSkip(final StateObj state) {
        return null != skipTriggerState && state.hasState(skipTriggerState);
    }

    @Override
    public EngineWorkflowStepOperationCompleted apply(MultiDataContext<String,DataContext> context)  {
        didRun = true;
        BaseWorkflowExecutor.StepResultCapture stepResultCapture = callable.apply(context);
        StepExecutionResult result = stepResultCapture.getStepResult();
        ControlBehavior controlBehavior = stepResultCapture.getControlBehavior();
        String statusString = stepResultCapture.getStatusString();

        EngineWorkflowExecutor.logger.debug("StepOperation callable complete: " + stepResultCapture);

        MutableStateObj stateChanges = States.mutable();
        boolean success = null != result && result.isSuccess();
        if (result != null) {
            EngineWorkflowExecutor.updateStateWithStepResultData(
                    stateChanges,
                    stepNum,
                    result.getResultData(),
                    result.getFailureData()
            );
        }
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        String stepResultValue = success
                                 ? EngineWorkflowExecutor.STEP_STATE_RESULT_SUCCESS
                                 : EngineWorkflowExecutor.STEP_STATE_RESULT_FAILURE;
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, stepNum),
                stepResultValue
        );
        if (label != null) {
            stateChanges.updateState(
                    EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, "label." + label),
                    stepResultValue
            );
            stateChanges.updateState(
                    EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, "label." + label),
                    EngineWorkflowExecutor.VALUE_TRUE
            );
            if (result != null) {
                EngineWorkflowExecutor.updateStateWithStepResultData(
                        stateChanges,
                        "label." + label,
                        result.getResultData(),
                        result.getFailureData()
                );
            }
        }
        if (success) {
            stateChanges.updateState(
                    EngineWorkflowExecutor.STEP_ANY_STATE_SUCCESS_KEY,
                    EngineWorkflowExecutor.VALUE_TRUE
            );
        } else {
            stateChanges.updateState(
                    EngineWorkflowExecutor.STEP_ANY_STATE_FAILED_KEY,
                    EngineWorkflowExecutor.VALUE_TRUE
            );
        }
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_BEFORE_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_AFTER_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );

        if (controlBehavior != null) {
            stateChanges.updateState(EngineWorkflowExecutor.stepKey(
                    EngineWorkflowExecutor.STEP_FLOW_CONTROL_KEY,
                    stepNum
            ), controlBehavior.toString());
            if (controlBehavior == ControlBehavior.Halt) {
                stateChanges.updateState(
                        EngineWorkflowExecutor.STEP_ANY_FLOW_CONTROL_HALT_KEY,
                        EngineWorkflowExecutor.VALUE_TRUE
                );
            }
            if (null != statusString) {
                stateChanges.updateState(EngineWorkflowExecutor.stepKey(
                        EngineWorkflowExecutor.STEP_FLOW_CONTROL_STATUS_KEY,
                        stepNum
                ), statusString);
            }
        }

        return new EngineWorkflowStepOperationCompleted(stepNum, stateChanges, stepResultCapture);
    }

    @Override
    public StateObj getSkipState(final StateObj state) {
        MutableStateObj stateChanges = States.mutable();
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, stepNum),
                EngineWorkflowExecutor.STEP_STATE_RESULT_SKIPPED
        );
        stateChanges.updateState(EngineWorkflowExecutor.STEP_ANY_STATE_SKIPPED_KEY, EngineWorkflowExecutor.VALUE_TRUE);
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_BEFORE_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_AFTER_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        return stateChanges;
    }

    @Override
    public StateObj getFailureState(final Throwable t) {
        MutableStateObj stateChanges = States.mutable();
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, stepNum),
                EngineWorkflowExecutor.STEP_STATE_RESULT_FAILURE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_BEFORE_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_AFTER_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        return stateChanges;
    }

    public boolean isDidRun() {
        return didRun;
    }

    @Override
    public String toString() {
        return "EngineWorkflowStepOperation{" +
               "stepNum=" + stepNum +
               ", label='" + label + '\'' +
               '}';
    }
}
