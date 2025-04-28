package me.Domplanto.streamLabs.action.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.AbstractStep;
import me.Domplanto.streamLabs.action.StepBase;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.yaml.PropertyBasedClassInitializer;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.logging.Level;

public abstract class AbstractQuery<T> implements StepBase<T> {
    @SuppressWarnings("rawtypes")
    public static final PropertyBasedClassInitializer<AbstractQuery> INITIALIZER = new PropertyBasedClassInitializer<>(AbstractQuery.class, "query", null);
    @YamlProperty("output")
    private String output;
    private ConfigPathStack path;

    @Override
    public void load(@NotNull T data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        this.path = issueHelper.stackCopy();
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException {
        try {
            String data = Objects.requireNonNullElse(this.runQuery(ctx, plugin), "");
            ctx.addSpecificPlaceholder(new QueryPlaceholder(this.output, data));
        } catch (Exception e) {
            StreamLabs.LOGGER.log(Level.WARNING, "Failed to run query for placeholder {>%s} at %s:".formatted(this.output, this.path.toFormattedString()), e);
        }
    }

    protected abstract @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin);

    private static class QueryPlaceholder extends ActionPlaceholder {
        public QueryPlaceholder(@NotNull String name, @NotNull String value) {
            super(name, PlaceholderFunction.of(value));
        }

        @Override
        public @NotNull String getFormat() {
            return "{>%s}".formatted(name());
        }
    }
}
