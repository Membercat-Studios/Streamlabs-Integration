package com.membercat.streamlabs.step.query;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.util.ReflectUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ReflectUtil.ClassId("function")
public class FunctionQuery extends AbstractQuery<String> {
    private static final String PARAMS_SECTION = "parameters";
    private String functionId;
    private Map<String, String> paramPlaceholders;

    @Override
    public @NotNull Class<String> getExpectedDataType() {
        return String.class;
    }

    @Override
    public void earlyLoad(@NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        String stepId = this.getStepId();
        this.paramPlaceholders = new HashMap<>();
        ConfigurationSection paramsSection = parent.getConfigurationSection(PARAMS_SECTION);
        if (paramsSection != null) {
            issueHelper.process(PARAMS_SECTION);
            for (String key : paramsSection.getKeys(false)) {
                if (key.equals(stepId)) continue;
                String content = Objects.requireNonNullElse(paramsSection.get(key), "").toString();
                this.paramPlaceholders.put(key, content);
            }
        }
        super.earlyLoad(issueHelper, parent);
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.functionId = data;
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamlabsIntegration plugin) {
        String functionId = AbstractPlaceholder.replacePlaceholders(this.functionId, ctx);
        PluginConfig.Function function = ctx.config().getFunction(functionId);
        if (function == null) {
            StreamlabsIntegration.LOGGER.warning("No function with ID \"%s\" (resolved from \"%s\") could be found at %s, skipping!".formatted(functionId, this.functionId, location().toFormattedString()));
            return null;
        }

        ctx.scopeStack().push("function %s".formatted(function));
        this.paramPlaceholders.entrySet()
                .stream().map(entry -> new FunctionParameterPlaceholder(entry.getKey(), AbstractPlaceholder.replacePlaceholders(entry.getValue(), ctx)))
                .forEach(pl -> ctx.scopeStack().addPlaceholder(pl));
        ctx.runSteps(function, plugin);

        String output = null;
        if (hasOutput() && function.getOutput() != null)
            output = AbstractPlaceholder.replacePlaceholders(function.getOutput(), ctx);
        else if (hasOutput())
            StreamlabsIntegration.LOGGER.warning("Tried to get output of function %s, but function doesn't have an output (at %s)".formatted(functionId, location().toFormattedString()));
        ctx.scopeStack().pop();
        return output;
    }

    @Override
    protected boolean isOptional() {
        return true;
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
