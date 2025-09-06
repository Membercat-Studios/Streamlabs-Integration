package me.Domplanto.streamLabs.action.collection;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class NamedCollection<T> {
    private final Map<String, Function<T, ?>> additionalProperties;
    private @Nullable NamedCollectionInstance.CollectionFilter<T> defaultFilter;

    protected NamedCollection() {
        this.additionalProperties = new HashMap<>();
    }

    public abstract @NotNull Stream<T> loadCollection(@NotNull Server server);

    public abstract @NotNull String getElementId(@NotNull T element);

    public abstract @Nullable Component getElementDisplayName(@NotNull T element);

    public @NotNull Component getElementDisplayNameSafe(@NotNull T element) {
        return Objects.requireNonNullElseGet(this.getElementDisplayName(element),
                () -> Component.text(this.getElementId(element).replaceAll("_", " ")));
    }

    public @NotNull String getElementDisplayNameString(@NotNull T element) {
        return PlainTextComponentSerializer.plainText().serialize(getElementDisplayNameSafe(element));
    }

    protected NamedCollection<T> withProperty(@NotNull String name, @NotNull Function<T, Object> propertyFunc) {
        this.additionalProperties.put(name, propertyFunc);
        return this;
    }

    protected NamedCollection<T> withDefaultFilter(@NotNull NamedCollectionInstance.CollectionFilter<T> filter) {
        this.defaultFilter = filter;
        return this;
    }

    protected NamedCollection<T> withDefaultFilter(@NotNull ConditionGroup filterGroup) {
        return this.withDefaultFilter(new NamedCollectionInstance.CollectionFilter<>(filterGroup));
    }

    public void applyDefaultFilters(@NotNull NamedCollectionInstance.CollectionFilter<T> filter) {
        if (this.defaultFilter != null) filter.merge(this.defaultFilter);
    }

    public @NotNull Map<String, Function<T, ?>> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public @NotNull String getPropertyAsString(@NotNull String propertyId, @NotNull T element) {
        if (!additionalProperties.containsKey(propertyId)) return "";
        Object o = additionalProperties.get(propertyId).apply(element);
        return switch (o) {
            case Component component -> PlainTextComponentSerializer.plainText().serialize(component);
            case TextColor color -> color.asHexString();
            case Keyed keyed -> keyed.key().asString();
            default -> o.toString();
        };
    }

    public static class SimpleCollection<T> extends NamedCollection<T> {
        private final Function<Server, Stream<T>> loadFunc;
        private final Function<T, String> idGetter;

        private Function<T, Component> nameGetter;

        protected SimpleCollection(@NotNull Function<Server, Stream<T>> loadFunc, @NotNull Function<T, String> idFunction) {
            this.loadFunc = loadFunc;
            this.idGetter = idFunction;
        }

        protected SimpleCollection<T> withNameFunction(@NotNull Function<T, Component> nameFunction) {
            this.nameGetter = nameFunction;
            return this;
        }

        @Override
        public @NotNull Stream<T> loadCollection(@NotNull Server server) {
            return Objects.requireNonNull(this.loadFunc.apply(server), "Collection contents can't be null");
        }

        @Override
        public @NotNull String getElementId(@NotNull T element) {
            return Objects.requireNonNull(this.idGetter.apply(element), "Collection element ID can't be null");
        }

        @Override
        public @Nullable Component getElementDisplayName(@NotNull T element) {
            return this.nameGetter != null ? this.nameGetter.apply(element) : null;
        }
    }

    public static class RegistryCollection<E extends Keyed, K extends RegistryKey<@NotNull E>> extends NamedCollection<E> {
        private final @NotNull K key;

        protected RegistryCollection(@NotNull K key) {
            this.key = key;
        }

        @SuppressWarnings("UnstableApiUsage")
        protected RegistryCollection<E, K> withTagProperty(@NotNull String name, TagKey<@NotNull E> tagKey) {
            return (RegistryCollection<E, K>) this.withProperty(name, element -> {
                Registry<@NotNull E> registry = RegistryAccess.registryAccess().getRegistry(tagKey.registryKey());
                return registry.getTag(tagKey).resolve(registry).contains(element);
            });
        }

        @Override
        public @NotNull Stream<E> loadCollection(@NotNull Server server) {
            return RegistryAccess.registryAccess().getRegistry(this.key).stream();
        }

        @Override
        public @NotNull String getElementId(@NotNull E element) {
            return element.getKey().asString();
        }

        @Override
        public @Nullable Component getElementDisplayName(@NotNull E element) {
            return element instanceof Translatable translatable ? Component.translatable(translatable) : null;
        }
    }
}
