package com.membercat.streamlabs.action;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.step.AbstractStep;
import com.membercat.streamlabs.step.StepBase;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Level;

public interface StepExecutor {
    @NotNull String getName();

    @SuppressWarnings("rawtypes")
    @NotNull Collection<? extends StepBase> getSteps(ActionExecutionContext ctx);

    default void runSteps(ActionExecutionContext ctx, StreamLabs plugin) {
        int id = 0;
        for (StepBase<?> step : this.getSteps(ctx)) {
            try {
                if (!ctx.shouldKeepExecuting() || step == null) return;
                step.execute(ctx, plugin);
            } catch (AbstractStep.ActionFailureException e) {
                ctx.markDirty();
                plugin.getLogger().log(Level.SEVERE, "Unexpected error while executing step %s (type: %s) in %s for event %s: %s".formatted(id, step.getStepId(), this.getName(), ctx.event().getId(), e.getMessage()), e.getCause());
                if (ctx.shouldStopOnFailure()) {
                    plugin.getLogger().info("Stopping step execution of %s due to stop_on_failure = true!".formatted(this.getName()));
                    return;
                }
            }
            id++;
        }
    }

    static @NotNull StepExecutor fromSteps(@NotNull String name, @NotNull Collection<? extends StepBase<?>> steps) {
        return new StepExecutor() {
            @Override
            public @NotNull String getName() {
                return name;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public @NotNull Collection<? extends StepBase> getSteps(ActionExecutionContext ctx) {
                return steps;
            }
        };
    }
}
