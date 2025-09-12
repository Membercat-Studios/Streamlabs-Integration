package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.collection.NamedCollection;
import me.Domplanto.streamLabs.action.collection.NamedCollectionInstance;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.placeholder.AbstractPlaceholder;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.Set;

@ReflectUtil.ClassId("random_element")
public class RandomElementQuery extends AbstractQuery<NamedCollectionInstance<?>> {
    private NamedCollectionInstance<?> collection;
    @YamlProperty("seed")
    private long seed;
    private Random random;

    @Override
    public void load(@NotNull NamedCollectionInstance<?> data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.collection = data;
        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    protected @Nullable AbstractPlaceholder query(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        //noinspection unchecked
        NamedCollection<Object> namedCollection = (NamedCollection<Object>) collection.collection();
        List<?> elements = collection.getElements(plugin.getServer()).toList();
        if (elements.isEmpty()) return new QueryPlaceholder(outputName(), "");

        int idx = random.nextInt(0, elements.size());
        return namedCollection.createPropertyPlaceholder(elements.get(idx), outputName(), QueryPlaceholder.FORMAT);
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        return null;
    }

    @Override
    public @NotNull Set<Serializer<?, NamedCollectionInstance<?>>> getOptionalDataSerializers() {
        return Set.of(NamedCollectionInstance.SERIALIZER);
    }

    @Override
    public @NotNull Class<NamedCollectionInstance<?>> getExpectedDataType() {
        return NamedCollectionInstance.CLS;
    }
}
