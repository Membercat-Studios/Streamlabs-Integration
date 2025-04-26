package me.Domplanto.streamLabs.action.ratelimiter;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;

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
    public boolean check(ActionExecutionContext ctx) {
        String value = getValue(ctx);
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
