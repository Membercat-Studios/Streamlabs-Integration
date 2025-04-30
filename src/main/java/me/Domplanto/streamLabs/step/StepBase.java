package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.util.yaml.PropertyLoadable;
import org.jetbrains.annotations.NotNull;

@ConfigPathSegment(id = "step")
public interface StepBase<T> extends PropertyLoadable<T> {
    void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException;
}
