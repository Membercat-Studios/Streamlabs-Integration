package com.membercat.streamlabs.condition;

import com.membercat.streamlabs.action.ActionExecutionContext;

public interface ConditionBase {
    boolean check(ActionExecutionContext ctx);
}
