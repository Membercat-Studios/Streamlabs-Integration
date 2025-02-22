package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;

public interface ConditionBase {
    boolean check(ActionExecutionContext ctx);

    class Default implements ConditionBase {
        @Override
        public boolean check(ActionExecutionContext ctx) {
            return false;
        }
    }
}
