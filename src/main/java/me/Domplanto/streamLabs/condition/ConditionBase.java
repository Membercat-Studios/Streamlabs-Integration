package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;

public interface ConditionBase {
    boolean check(ActionExecutionContext ctx);
}
