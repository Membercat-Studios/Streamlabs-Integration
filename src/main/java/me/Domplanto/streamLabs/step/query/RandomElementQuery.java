package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.NamedCollection;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.Set;

@ReflectUtil.ClassId("random_element")
public class RandomElementQuery extends AbstractQuery<NamedCollection> {
    private NamedCollection collection;
    @YamlProperty("seed")
    private long seed;
    private Random random;

    @Override
    public void load(@NotNull NamedCollection data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.collection = data;
        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        List<?> elements = collection.loadCollection(plugin.getServer()).toList();
        if (elements.isEmpty()) return null;

        int idx = random.nextInt(0, elements.size());
        return collection.getId(elements.get(idx));
    }

    @Override
    public @NotNull Set<Serializer<?, NamedCollection>> getOptionalDataSerializers() {
        return Set.of(NamedCollection.SERIALIZER);
    }

    @Override
    public @NotNull Class<NamedCollection> getExpectedDataType() {
        return NamedCollection.class;
    }
}
