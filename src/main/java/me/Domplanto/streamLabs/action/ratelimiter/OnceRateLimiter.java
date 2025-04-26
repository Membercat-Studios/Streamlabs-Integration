package me.Domplanto.streamLabs.action.ratelimiter;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.util.ReflectUtil;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@ReflectUtil.ClassId("once")
public class OnceRateLimiter extends RateLimiter {
    private final Set<String> values = new HashSet<>();

    @Override
    public boolean check(ActionExecutionContext ctx) {
        return this.values.add(getValue(ctx));
    }

    @Override
    public void reset() {
        this.values.clear();
    }
}
