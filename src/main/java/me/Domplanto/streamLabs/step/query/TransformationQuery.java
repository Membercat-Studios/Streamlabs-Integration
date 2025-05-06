package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.Domplanto.streamLabs.config.issue.Issues.WQ1;

public abstract class TransformationQuery<T> extends AbstractQuery<T> {
    @YamlProperty("input")
    private String input;

    @YamlPropertyIssueAssigner(propertyName = "input")
    private void assignToInput(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (!actuallySet) issueHelper.appendAtPath(WQ1);
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        if (input == null) return null;
        String input = ActionPlaceholder.replacePlaceholders(this.input, ctx);
        return this.runQuery(input, ctx, plugin);
    }

    protected abstract @Nullable String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin);
}
