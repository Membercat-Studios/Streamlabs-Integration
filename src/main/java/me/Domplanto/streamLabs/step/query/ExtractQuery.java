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

@ReflectUtil.ClassId("extract")
public class ExtractQuery extends TransformationQuery<NamedCollectionInstance<?>> {
    private NamedCollectionInstance<?> extractCollection;

    @Override
    public void load(@NotNull NamedCollectionInstance<?> data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.extractCollection = data;
    }

    @Override
    protected @Nullable AbstractPlaceholder query(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        //noinspection unchecked
        NamedCollection<Object> collection = (NamedCollection<Object>) this.extractCollection.collection();
        Object element = this.extract(input, plugin.getServer(), ctx, collection);
        if (element == null) return createPlaceholder(null);
        return collection.createPropertyPlaceholder(element, outputName(), QueryPlaceholder.FORMAT);
    }

    @Override
    protected @Nullable String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        return null;
    }

    private @Nullable Object extract(String input, Server server, ActionExecutionContext ctx, @NotNull NamedCollection<Object> collection) {
        String lowerInput = input.toLowerCase();
        return this.extractCollection.getElements(server, ctx)
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
