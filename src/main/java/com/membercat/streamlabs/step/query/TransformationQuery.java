package com.membercat.streamlabs.step.query;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import com.membercat.streamlabs.util.yaml.YamlPropertyIssueAssigner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.membercat.streamlabs.config.issue.Issues.WQ1;

public abstract class TransformationQuery<T> extends AbstractQuery<T> {
    @YamlProperty("input")
    private String input;

    @YamlPropertyIssueAssigner(propertyName = "input")
    private void assignToInput(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (!actuallySet) issueHelper.appendAtPath(WQ1);
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        return null;
    }

    @Override
    protected @Nullable AbstractPlaceholder query(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        if (input == null) return null;
        String input = AbstractPlaceholder.replacePlaceholders(this.input, ctx);
        return this.query(input, ctx, plugin);
    }

    protected @Nullable AbstractPlaceholder query(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        String data = this.runQuery(input, ctx, plugin);
        return this.hasOutput() ? this.createPlaceholder(data) : null;
    }

    protected abstract @Nullable String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin);
}
