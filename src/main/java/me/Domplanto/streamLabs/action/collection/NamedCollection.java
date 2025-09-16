package me.Domplanto.streamLabs.action.collection;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.Tag;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.placeholder.PropertyPlaceholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
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
        Component safe = this.getElementDisplayNameSafe(element);
        return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(safe, Locale.getDefault()));
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
        Map<String, Function<T, ?>> props = new HashMap<>(this.additionalProperties);
        props.put("display_name", this::getElementDisplayName);
        props.put("display_name_safe", this::getElementDisplayNameSafe);
        return props;
    }

    public @NotNull PropertyPlaceholder createPropertyPlaceholder(@NotNull T element, @NotNull String outputName, @NotNull String format) {
        PropertyPlaceholder placeholder = new PropertyPlaceholder(outputName, format)
                .withDefaultValue(getElementId(element));
        for (Map.Entry<String, Function<T, ?>> entry : getAdditionalProperties().entrySet()) {
            try {
                Object result = entry.getValue().apply(element);
                if (result instanceof Component component) {
                    String formatted = MiniMessage.miniMessage().serialize(component);
                    placeholder.addProperty(entry.getKey() + ":formatted", formatted);
                }

                placeholder.addProperty(entry.getKey(), getPropertyAsString(result));
            } catch (Throwable e) {
                StreamLabs.LOGGER.log(Level.WARNING, "Failed to read property \"%s\" of named collection entry:", e);
            }
        }
        return placeholder;
    }

    public static @NotNull String getPropertyAsString(@Nullable Object property) {
        if (property == null) return "";
        return switch (property) {
            case Component component -> PlainTextComponentSerializer.plainText()
                    .serialize(GlobalTranslator.render(component, Locale.getDefault()));
            case TextColor color -> color.asHexString();
            case Keyed keyed -> keyed.key().asString();
            case Enum<?> en -> en.name().toLowerCase();
            default -> property.toString();
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

        @Override
        @SuppressWarnings("UnstableApiUsage")
        public @NotNull Map<String, Function<E, ?>> getAdditionalProperties() {
            Map<String, Function<E, ?>> props = new HashMap<>(super.getAdditionalProperties());
            for (Tag<@NotNull E> tag : registry().getTags()) {
                String key = tag.tagKey().key().asMinimalString();
                props.put("tag:" + key, element -> tag.contains(TypedKey.create(this.key, element.key())));
            }
            return props;
        }

        @Override
        public @NotNull Stream<E> loadCollection(@NotNull Server server) {
            return registry().stream();
        }

        private @NotNull Registry<@NotNull E> registry() {
            return RegistryAccess.registryAccess().getRegistry(this.key);
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
