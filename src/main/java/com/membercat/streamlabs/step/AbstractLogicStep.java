package com.membercat.streamlabs.step;

import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.action.StepExecutor;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.step.query.AbstractQuery;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.membercat.streamlabs.config.issue.Issues.WPI2;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicStep extends AbstractStep<List> implements StepExecutor {
    private List<? extends StepBase<?>> steps = new ArrayList<>();
    private String name;
    @YamlProperty("server_thread")
    private boolean runOnServerThread = false;

    public AbstractLogicStep() {
        super(List.class);
    }

    protected static List<? extends StepBase<?>> loadSteps(@NotNull List<?> data, Class<?> expected, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        try {
            //noinspection unchecked
            return AbstractStep.INITIALIZER.parseAll((List<Object>) data, parent, issueHelper)
                    .stream().map(step -> (StepBase<?>) step).toList();
        } catch (ClassCastException e) {
            issueHelper.appendAtPath(WPI2(AbstractStep.NAME_ID, expected, data));
            return new ArrayList<>();
        }
    }

    @Override
    public void load(@NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.name = "%s step at %s".formatted(getStepId(), getLocation().toFormattedString());
        this.steps = loadSteps(data, getExpectedDataType(), issueHelper, parent);
    }

    @Override
    protected void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        Runnable action = () -> ctx.runSteps(this, getPlugin());
        if (isAsync(ctx)) {
            action.run();
            return;
        }
        try {
            AbstractQuery.runOnServerThread(getPlugin(), Long.MAX_VALUE, action);
        } catch (TimeoutException e) {
            throw new ActionFailureException("Timeout while running logic step", e);
        }
    }

    public List<? extends StepBase<?>> steps() {
        return this.steps;
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    protected boolean isAsync(@SuppressWarnings("unused") ActionExecutionContext ctx) {
        return !this.runOnServerThread;
    }

    @Override
    public @NotNull Collection<? extends StepBase<?>> getSteps(ActionExecutionContext ctx) {
        return this.steps();
    }
}
