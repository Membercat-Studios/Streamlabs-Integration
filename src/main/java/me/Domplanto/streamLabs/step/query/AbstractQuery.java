package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.config.placeholder.AbstractPlaceholder;
import me.Domplanto.streamLabs.config.placeholder.ActionPlaceholder;
import me.Domplanto.streamLabs.step.AbstractStep;
import me.Domplanto.streamLabs.step.StepBase;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static me.Domplanto.streamLabs.config.issue.Issues.WQ0;

@ConfigPathSegment(id = "query")
public abstract class AbstractQuery<T> implements StepBase<T> {
    @YamlProperty("output")
    private String output;
    private ConfigPathStack path;

    @Override
    public void load(@NotNull T data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        this.path = issueHelper.stackCopy();
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws AbstractStep.ActionFailureException {
        if (invalid()) return;
        try {
            AbstractPlaceholder result = this.query(ctx, plugin);
            if (hasOutput()) ctx.scopeStack().addPlaceholder(Objects.requireNonNull(result));
        } catch (Exception e) {
            throw new AbstractStep.ActionFailureException("An unexpected internal error occurred", e);
        }
    }

    public static void runOnServerThread(@NotNull JavaPlugin plugin, long timeout, Runnable action) throws TimeoutException {
        runOnServerThread(plugin, timeout, () -> {
            action.run();
            return new Object();
        });
    }

    public static <T> T runOnServerThread(@NotNull JavaPlugin plugin, long timeout, Supplier<T> action) throws TimeoutException {
        if (Bukkit.isPrimaryThread()) return action.get();
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                T result = action.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Server thread action interrupted");
        } catch (ExecutionException e) {
            throw new RuntimeException("Error while executing action on server thread", e);
        }
    }

    protected @Nullable AbstractPlaceholder query(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        String data = Objects.requireNonNullElse(this.runQuery(ctx, plugin), "");
        if (!this.hasOutput()) return null;
        return new QueryPlaceholder(this.output, data);
    }

    protected abstract @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin);

    protected ConfigPathStack location() {
        return this.path;
    }

    protected boolean hasOutput() {
        return this.output != null;
    }

    protected String outputName() {
        return this.output;
    }

    private boolean invalid() {
        return !hasOutput() && !isOptional();
    }

    protected boolean isOptional() {
        return false;
    }

    @YamlPropertyIssueAssigner(propertyName = "output")
    public void assignToOutput(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (invalid()) issueHelper.appendAtPath(WQ0);
    }

    public static class QueryPlaceholder extends ActionPlaceholder {
        public static final String FORMAT = "\\{\\$%s\\}";

        public QueryPlaceholder(@NotNull String name, @NotNull String value) {
            super(name, PlaceholderFunction.of(value));
        }

        @Override
        public @NotNull String getFormat() {
            return FORMAT.replaceAll("\\\\", "").formatted(name());
        }
    }
}
