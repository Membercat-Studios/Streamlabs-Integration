package com.membercat.streamlabs.step;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigPathSegment;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.PropertyLoadable;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@ConfigPathSegment(id = "step")
public interface StepBase<T> extends PropertyLoadable<T> {
    void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException;

    default @NotNull String getStepId() {
        return Optional.ofNullable(getClass().getAnnotation(ReflectUtil.ClassId.class))
                .map(ReflectUtil.ClassId::value).orElse("unknown");
    }

    static @NotNull StepBase<?> createExecuting(@NotNull FailableBiConsumer<ActionExecutionContext, StreamLabs, AbstractStep.ActionFailureException> action) {
        return new StepBase<>() {
            @Override
            public void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException {
                action.accept(ctx, plugin);
            }

            @Override
            public @NotNull Class<Object> getExpectedDataType() {
                return Object.class;
            }

            @Override
            public void load(@NotNull Object data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
            }
        };
    }
}
