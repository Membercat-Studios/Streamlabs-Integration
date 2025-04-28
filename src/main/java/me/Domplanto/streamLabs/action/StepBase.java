package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.util.yaml.PropertyLoadable;
import org.jetbrains.annotations.NotNull;

public interface StepBase<T> extends PropertyLoadable<T> {
    void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException;
}
