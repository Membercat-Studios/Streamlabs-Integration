package me.Domplanto.streamLabs.step;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.placeholder.AbstractPlaceholder;
import me.Domplanto.streamLabs.step.query.AbstractQuery;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static me.Domplanto.streamLabs.config.issue.Issues.WV0;

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
