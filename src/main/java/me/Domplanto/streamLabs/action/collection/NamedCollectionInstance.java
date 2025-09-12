package me.Domplanto.streamLabs.action.collection;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.placeholder.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.statistics.EventHistorySelector;
import me.Domplanto.streamLabs.util.yaml.PropertyLoadable;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.WNC0;

@SuppressWarnings("ClassCanBeRecord")
public final class NamedCollectionInstance<T> {
    public static final PropertyLoadable.Serializer<String, NamedCollectionInstance<?>> SERIALIZER;
    public static final NamedCollectionInstance<?> EMPTY_INSTANCE;
    @SuppressWarnings("unchecked")
    public static final Class<NamedCollectionInstance<?>> CLS = (Class<NamedCollectionInstance<?>>) (Class<?>) NamedCollectionInstance.class;
    private final @NotNull NamedCollection<T> collection;
    private final @NotNull CollectionFilter<T> filter;

    private NamedCollectionInstance(@NotNull NamedCollection<T> collection, @NotNull CollectionFilter<T> filter) {
        this.collection = collection;
        this.filter = filter;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static @NotNull NamedCollectionInstance<?> parse(@NotNull String input, ConfigIssueHelper issueHelper) {
        Pair<String, ConditionGroup> data = EventHistorySelector.parseOptionalFilter(input, issueHelper);
        NamedCollection namedCollection = Collections.fromName(data.getKey());
        if (namedCollection == null) {
            namedCollection = Collections.EMPTY;
            issueHelper.appendAtPath(WNC0.apply(data.getKey()));
        }

        CollectionFilter<?> filter = new CollectionFilter<>(data.getValue());
        namedCollection.applyDefaultFilters(filter);
        return new NamedCollectionInstance<>(namedCollection, filter);
    }

    public @NotNull Stream<T> getElements(@NotNull Server server) {
        return this.filter.applyOn(this.collection.loadCollection(server), collection);
    }

    public @NotNull NamedCollection<T> collection() {
        return this.collection;
    }

    public static class CollectionFilter<T> {
        private ConditionGroup conditionGroup;

        protected CollectionFilter(@NotNull ConditionGroup conditionGroup) {
            this.conditionGroup = conditionGroup;
        }

        public void merge(@NotNull CollectionFilter<T> from) {
            this.conditionGroup = ConditionGroup.of(ConditionGroup.Mode.AND, from.conditionGroup, this.conditionGroup);
        }

        public @NotNull Stream<T> applyOn(@NotNull Stream<T> data, @NotNull NamedCollection<T> source) {
            return data.filter(element -> {
                ActionExecutionContext groupCtx = new ActionExecutionContext(null, null, null, null, false, new JsonObject());
                groupCtx.scopeStack().addPlaceholder("id", ActionPlaceholder.PlaceholderFunction.of(o -> source.getElementId(element)));
                for (Map.Entry<String, Function<T, ?>> entry : source.getAdditionalProperties().entrySet()) {
                    groupCtx.scopeStack().addPlaceholder(entry.getKey(), ActionPlaceholder.PlaceholderFunction.of(o ->
                            NamedCollection.getPropertyAsString(entry.getValue().apply(element))));
                }
                return this.conditionGroup.check(groupCtx);
            });
        }
    }

    static {
        SERIALIZER = new PropertyLoadable.Serializer<>(String.class, CLS, NamedCollectionInstance::parse);
        EMPTY_INSTANCE = new NamedCollectionInstance<>(Collections.EMPTY, new CollectionFilter<>(ConditionGroup.of(ConditionGroup.Mode.AND)));
    }
}