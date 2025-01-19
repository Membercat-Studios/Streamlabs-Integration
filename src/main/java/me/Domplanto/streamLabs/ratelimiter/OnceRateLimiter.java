package me.Domplanto.streamLabs.ratelimiter;

import me.Domplanto.streamLabs.action.ActionExecutionContext;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class OnceRateLimiter extends RateLimiter {
    private final Set<String> values;
    public OnceRateLimiter() {
        super("once");
        this.values = new HashSet<>();
    }

    @Override
    public boolean check(ActionExecutionContext ctx) {
        return this.values.add(getValue(ctx));
    }

    @Override
    public void reset() {
        this.values.clear();
    }
}
