package com.membercat.streamlabs.action.ratelimiter;

import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@ReflectUtil.ClassId("once")
public class OnceRateLimiter extends RateLimiter {
    private final Set<String> values = new HashSet<>();

    @Override
    public boolean check(@NotNull String value, ActionExecutionContext ctx) {
        return this.values.add(value);
    }

    @Override
    public void reset() {
        this.values.clear();
    }
}
