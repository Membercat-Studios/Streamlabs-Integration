package com.membercat.streamlabs.step;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.placeholder.AbstractPlaceholder;
import com.membercat.streamlabs.step.query.AbstractQuery;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.yaml.BracketResolver;
import com.membercat.streamlabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static com.membercat.streamlabs.config.issue.Issues.WV0;

@ReflectUtil.ClassId("set_placeholder")
public class SetPlaceholderStep extends AbstractStep<BracketResolver> {
    private @Nullable String placeholderName;
    private String value;
    @YamlProperty("ignore_placeholders")
    private boolean ignorePlaceholders;

    public SetPlaceholderStep() {
        super(BracketResolver.class);
    }

    @Override
    public void load(@NotNull BracketResolver data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        data.resolve(issueHelper);
        this.value = data.getContent();
        this.placeholderName = data.getBracketContents().orElseGet(() -> {
            issueHelper.appendAtPath(WV0);
            return null;
        });
    }

    @Override
    protected void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        if (placeholderName == null) return;
        String content = !ignorePlaceholders ? AbstractPlaceholder.replacePlaceholders(value, ctx) : value;
        try {
            double value = new DoubleEvaluator().evaluate(content);
            content = (value == (int) value) ? String.valueOf((int) value) : String.valueOf(value);
        } catch (Exception ignore) {
        }
        ctx.scopeStack().addPlaceholder(new VariablePlaceholder(this.placeholderName, content));
    }

    @Override
    public @NotNull Set<Serializer<?, BracketResolver>> getOptionalDataSerializers() {
        return Set.of(new Serializer<>(String.class, BracketResolver.class, (s, helper) -> new BracketResolver(s)));
    }

    public static class VariablePlaceholder extends AbstractQuery.QueryPlaceholder {
        public VariablePlaceholder(@NotNull String name, @NotNull String value) {
            super(name, value);
        }

        @Override
        public @NotNull String getFormat() {
            return "{#%s}".formatted(name());
        }
    }
}
