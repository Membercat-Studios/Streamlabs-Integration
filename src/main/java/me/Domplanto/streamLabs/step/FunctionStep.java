package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.query.AbstractQuery;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.PluginConfig;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ReflectUtil.ClassId("function")
public class FunctionStep extends AbstractStep<String> {
    private static final String PARAMS_SECTION = "parameters";
    private String functionId;
    private Map<String, String> paramPlaceholders;

    public FunctionStep() {
        super(String.class);
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.functionId = data;
        this.paramPlaceholders = new HashMap<>();
        String stepId = this.getStepId();
        ConfigurationSection paramsSection = parent.getConfigurationSection(PARAMS_SECTION);
        if (paramsSection == null) return;

        issueHelper.stack().get(issueHelper.stack().size() - 2).process(PARAMS_SECTION);
        for (String key : paramsSection.getKeys(false)) {
            if (key.equals(stepId)) continue;
            String content = Objects.requireNonNullElse(paramsSection.get(key), "").toString();
            this.paramPlaceholders.put(key, content);
        }
    }

    @Override
    protected void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        String functionId = ActionPlaceholder.replacePlaceholders(this.functionId, ctx);
        PluginConfig.Function function = ctx.config().getFunction(functionId);
        if (function == null) {
            StreamLabs.LOGGER.warning("No function with ID \"%s\" (resolved from \"%s\") could be found at %s, skipping!".formatted(functionId, this.functionId, getLocation().toFormattedString()));
            return;
        }

        this.paramPlaceholders.entrySet()
                .stream().map(entry -> new FunctionParameterPlaceholder(entry.getKey(), ActionPlaceholder.replacePlaceholders(entry.getValue(), ctx)))
                .forEach(ctx::addSpecificPlaceholder);
        ctx.runSteps(function, getPlugin());
    }

    public static class FunctionParameterPlaceholder extends AbstractQuery.QueryPlaceholder {
        public FunctionParameterPlaceholder(@NotNull String name, @NotNull String value) {
            super(name, value);
        }

        @Override
        public @NotNull String getFormat() {
            return "{$$%s}".formatted(name());
        }
    }
}
