package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;

import static me.Domplanto.streamLabs.config.issue.Issues.WD0;

@ReflectUtil.ClassId("delay")
public class DelayStep extends AbstractStep<Integer> {
    private int delay;

    public DelayStep() {
        super(Integer.class);
    }

    @Override
    public void load(@NotNull Integer data, @NotNull ConfigIssueHelper issueHelper) {
        if (data < 1) {
            this.delay = 1000;
            issueHelper.appendAtPath(WD0.apply(data, this.delay));
            return;
        }

        this.delay = data;
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        try {
            Thread.sleep(this.delay);
        } catch (InterruptedException ignore) {
        }
    }
}
