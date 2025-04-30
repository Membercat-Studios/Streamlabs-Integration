package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.PropertyLoadable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@ConfigPathSegment(id = "step")
public interface StepBase<T> extends PropertyLoadable<T> {
    void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException;

    default @NotNull String getStepId() {
        return Optional.ofNullable(getClass().getAnnotation(ReflectUtil.ClassId.class))
                .map(ReflectUtil.ClassId::value).orElse("unknown");
    }
}
