package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;

public interface ConditionBase {
    boolean check(ActionExecutionContext ctx);
}
