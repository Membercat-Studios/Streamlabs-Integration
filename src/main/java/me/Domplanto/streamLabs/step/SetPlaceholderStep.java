package me.Domplanto.streamLabs.step;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.query.AbstractQuery;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static me.Domplanto.streamLabs.config.issue.Issues.WV0;

@ReflectUtil.ClassId("set_placeholder")
public class SetPlaceholderStep extends AbstractStep<BracketResolver> {
    private @Nullable String placeholderName;
    private String value;

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
        String content = ActionPlaceholder.replacePlaceholders(this.value, ctx);
        try {
            content = new DoubleEvaluator().evaluate(content).toString();
        } catch (Exception ignore) {
        }
        ctx.addSpecificPlaceholder(new VariablePlaceholder(this.placeholderName, content));
    }

    @Override
    public @NotNull Set<Serializer<?, BracketResolver>> getOptionalDataSerializers() {
        return Set.of(new Serializer<>(String.class, BracketResolver.class, BracketResolver::new));
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
