package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.PropertyLoadable;
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
