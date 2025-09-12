package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.collection.NamedCollection;
import me.Domplanto.streamLabs.action.collection.NamedCollectionInstance;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.placeholder.AbstractPlaceholder;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.TimeoutException;

@ReflectUtil.ClassId("extract")
public class ExtractQuery extends TransformationQuery<NamedCollectionInstance<?>> {
    private static final long TIMEOUT = 10000;
    private NamedCollectionInstance<?> extractCollection;

    @Override
    public void load(@NotNull NamedCollectionInstance<?> data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.extractCollection = data;
    }

    @Override
    protected @Nullable AbstractPlaceholder query(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        try {
            return runOnServerThread(plugin, TIMEOUT, () -> {
                //noinspection unchecked
                NamedCollection<Object> collection = (NamedCollection<Object>) this.extractCollection.collection();
                Object element = this.extract(input, plugin.getServer(), collection);
                if (element == null) return createPlaceholder(null);
                return collection.createPropertyPlaceholder(element, outputName(), QueryPlaceholder.FORMAT);
            });
        } catch (TimeoutException e) {
            StreamLabs.LOGGER.warning("Extract query for type %s couldn't extract the correct value in time, action took >10000ms (tried to extract from: \"%s\")!".formatted(this.extractCollection, input));
            return null;
        }
    }

    @Override
    protected @Nullable String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        return null;
    }

    private @Nullable Object extract(String input, Server server, @NotNull NamedCollection<Object> collection) {
        String lowerInput = input.toLowerCase();
        return this.extractCollection.getElements(server)
                .filter(e -> lowerInput.contains(collection.getElementDisplayNameString(e).toLowerCase()))
                .findFirst().orElse(null);
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
