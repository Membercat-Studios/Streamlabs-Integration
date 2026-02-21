package com.membercat.streamlabs.action.ratelimiter;

import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;

@SuppressWarnings("unused")
@ReflectUtil.ClassId("interval")
public class IntervalRateLimiter extends RateLimiter {
    @YamlProperty("interval")
    private double intervalSeconds = 10;
    @YamlProperty("reset_while_pending")
    private boolean resetWhilePending = true;
    private final HashMap<String, Long> timestampedValues = new HashMap<>();

    @Override
    public boolean check(@NotNull String value, ActionExecutionContext ctx) {
        long currentDate = new Date().getTime();
        Long timestamp = timestampedValues.putIfAbsent(value, currentDate);
        if (timestamp == null) return true;
        boolean intervalElapsed = currentDate - timestamp >= (this.intervalSeconds * 1000);
        if (intervalElapsed || this.resetWhilePending)
            timestampedValues.replace(value, currentDate);
        return intervalElapsed;
    }

    @Override
    public void reset() {
        this.timestampedValues.clear();
    }
}
