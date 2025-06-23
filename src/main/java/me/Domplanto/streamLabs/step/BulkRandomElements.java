package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.NamedCollection;
import me.Domplanto.streamLabs.action.StepExecutor;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.step.query.AbstractQuery;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static me.Domplanto.streamLabs.config.issue.Issues.WNC1;

@ReflectUtil.ClassId("bulk_random_elements")
public class BulkRandomElements extends RepeatStep {
    @YamlProperty("collection")
    private NamedCollection collection = NamedCollection.NONE;
    @YamlProperty("seed")
    private long seed;
    private Random random;
    private @Nullable List<?> elements;

    @Override
    public void load(@SuppressWarnings("rawtypes") @NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    protected void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        this.elements = collection.loadCollection(getPlugin().getServer()).toList();
        if (elements.isEmpty()) return;
        ctx.scopeStack().push("bulk random element loop at %s".formatted(getLocation().toFormattedString()));
        super.execute(ctx);
        ctx.scopeStack().pop();
    }

    @Override
    public List<? extends StepBase<?>> steps() {
        return List.of(StepBase.createExecuting((ctx, plugin) -> {
            if (elements == null) return;
            int idx = random.nextInt(0, elements.size());
            Object o = this.elements.get(idx);
            ctx.scopeStack().addPlaceholder(new AbstractQuery.QueryPlaceholder("element_id", collection.getId(o)));
            ctx.scopeStack().addPlaceholder(new AbstractQuery.QueryPlaceholder("element_name", collection.getName(o)));
            ctx.runSteps(StepExecutor.fromSteps(this.getName(), super.steps()), getPlugin());
        }));
    }

    @YamlPropertyCustomDeserializer(propertyName = "collection")
    public @NotNull NamedCollection serializeCollection(@NotNull String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        return Objects.requireNonNull(NamedCollection.SERIALIZER.serialize(input, issueHelper));
    }

    @YamlPropertyIssueAssigner(propertyName = "collection")
    public void assignToCollection(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (!actuallySet) issueHelper.appendAtPath(WNC1);
    }
}
