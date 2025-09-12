package me.Domplanto.streamLabs.step;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.StepExecutor;
import me.Domplanto.streamLabs.action.collection.NamedCollection;
import me.Domplanto.streamLabs.action.collection.NamedCollectionInstance;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.placeholder.PropertyPlaceholder;
import me.Domplanto.streamLabs.step.query.AbstractQuery;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyIssueAssigner;
import org.apache.commons.lang3.function.TriFunction;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import static me.Domplanto.streamLabs.config.issue.Issues.WBS0;
import static me.Domplanto.streamLabs.config.issue.Issues.WNC1;

@ReflectUtil.ClassId("bulk_random_elements")
public class BulkRandomElements extends RepeatStep {
    private static final int MAX_TRIES = 10000;
    @YamlProperty("collection")
    private NamedCollectionInstance<?> collection = NamedCollectionInstance.EMPTY_INSTANCE;
    @YamlProperty("selection_behavior")
    private SelectionBehavior selectionBehavior = SelectionBehavior.NORMAL;
    @YamlProperty("seed")
    private long seed;
    private Random random;
    private @Nullable List<?> elements;
    private List<Object> selectedElements;
    private int step;

    @Override
    public void load(@SuppressWarnings("rawtypes") @NotNull List data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.random = seed != 0 ? new Random(seed) : new Random();
        this.selectedElements = new ArrayList<>();
    }

    @Override
    protected void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException {
        this.selectedElements.clear();
        this.step = 0;
        this.elements = collection.getElements(getPlugin().getServer()).toList();
        if (elements.isEmpty()) return;

        ctx.scopeStack().push("bulk random element loop at %s".formatted(getLocation().toFormattedString()));
        super.execute(ctx);
        ctx.scopeStack().pop();
        this.elements = null;
    }

    @Override
    public List<? extends StepBase<?>> steps() {
        //noinspection unchecked
        NamedCollection<Object> namedCollection = (NamedCollection<Object>) collection.collection();
        return List.of(StepBase.createExecuting((ctx, plugin) -> {
            if (elements == null) return;
            boolean overflow = this.step++ >= elements.size();
            Object o = this.selectionBehavior.selectNew(() -> {
                int idx = random.nextInt(0, elements.size());
                return this.elements.get(idx);
            }, overflow, this.selectedElements);
            if (o == null) return;
            this.selectedElements.add(o);

            PropertyPlaceholder placeholder = namedCollection.createPropertyPlaceholder(o, "element", AbstractQuery.QueryPlaceholder.FORMAT);
            ctx.scopeStack().addPlaceholder(placeholder);
            ctx.runSteps(StepExecutor.fromSteps(this.getName(), super.steps()), getPlugin());
        }));
    }

    @YamlPropertyCustomDeserializer(propertyName = "collection")
    public @NotNull NamedCollectionInstance<?> serializeCollection(@NotNull String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        return Objects.requireNonNull(NamedCollectionInstance.SERIALIZER.serialize(input, issueHelper));
    }

    @YamlPropertyCustomDeserializer(propertyName = "selection_behavior")
    public @NotNull SelectionBehavior serializeSelectionBehavior(@NotNull String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        try {
            return SelectionBehavior.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            issueHelper.appendAtPath(WBS0.apply(input));
            return SelectionBehavior.NORMAL;
        }
    }

    @YamlPropertyIssueAssigner(propertyName = "collection")
    public void assignToCollection(ConfigIssueHelper issueHelper, boolean actuallySet) {
        if (!actuallySet) issueHelper.appendAtPath(WNC1);
    }

    public enum SelectionBehavior {
        NORMAL((newObj, overflow, selected) -> newObj.get()),
        TRY_NO_DUPLICATES((newObj, overflow, selected) -> overflow ? newObj.get() : attemptSelect(newObj, selected)),
        FORCE_NO_DUPLICATES((newObj, overflow, selected) -> overflow ? null : attemptSelect(newObj, selected));
        private final TriFunction<Supplier<Object>, Boolean, List<Object>, Object> generator;

        SelectionBehavior(TriFunction<Supplier<Object>, Boolean, List<Object>, Object> generator) {
            this.generator = generator;
        }

        public @Nullable Object selectNew(@NotNull Supplier<Object> randomGenerator, boolean overflow, @NotNull List<Object> chosen) {
            return this.generator.apply(randomGenerator, overflow, chosen);
        }

        private static @Nullable Object attemptSelect(Supplier<Object> randomGenerator, List<Object> chosen) {
            int tries = 0;
            Object o;
            do {
                o = randomGenerator.get();
                tries++;
                if (tries > MAX_TRIES) return null;
            } while (chosen.contains(o));
            return o;
        }
    }
}
