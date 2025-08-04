package me.Domplanto.streamLabs.action;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.Domplanto.streamLabs.util.yaml.PropertyLoadable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.Keyed;
import org.bukkit.Server;
import org.bukkit.block.BlockType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.WNC0;

@SuppressWarnings("UnstableApiUsage")
public enum NamedCollection {
    NONE(server -> Stream.of(), e -> ""),
    PLAYER(server -> server.getOnlinePlayers().stream(), Player::getName),
    ENTITY_TYPE(server -> fromRegistry(RegistryKey.ENTITY_TYPE, EntityType::isSpawnable)),
    LIVING_ENTITY_TYPE(server -> fromRegistry(RegistryKey.ENTITY_TYPE, type -> type.isSpawnable() && type.isAlive())),
    ENCHANTMENT(server -> fromRegistry(RegistryKey.ENCHANTMENT, null)),
    STATUS_EFFECT(server -> fromRegistry(RegistryKey.MOB_EFFECT, null), NamedCollection::translatable, NamedCollection::keyedId),
    ITEM(server -> fromRegistry(RegistryKey.ITEM, null), NamedCollection::translatable, NamedCollection::keyedId),
    BLOCK(server -> fromRegistry(RegistryKey.BLOCK, null), NamedCollection::translatable, NamedCollection::keyedId),
    NON_AIR_BLOCK(server -> fromRegistry(RegistryKey.BLOCK, Predicate.not(BlockType::isAir)), NamedCollection::translatable, NamedCollection::keyedId),
    SOLID_BLOCK(server -> fromRegistry(RegistryKey.BLOCK, BlockType::isSolid), NamedCollection::translatable, NamedCollection::keyedId),
    BIOME(server -> fromRegistry(RegistryKey.BIOME, null), NamedCollection::translatable, NamedCollection::keyedId),
    STRUCTURE(server -> fromRegistry(RegistryKey.STRUCTURE, null));

    public final static PropertyLoadable.Serializer<String, NamedCollection> SERIALIZER;
    private final Function<Server, ?> loadFunc;
    private final Function<?, String> nameGetter;
    private final Function<?, String> idGetter;

    <T> NamedCollection(Function<Server, Stream<T>> loadFunc, Function<T, String> nameGetter, Function<T, String> idGetter) {
        this.loadFunc = loadFunc;
        this.nameGetter = nameGetter;
        this.idGetter = idGetter;
    }

    <T> NamedCollection(Function<Server, Stream<T>> loadFunc, Function<T, String> nameGetter) {
        this(loadFunc, nameGetter, nameGetter);
    }

    <T extends Keyed> NamedCollection(Function<Server, Stream<T>> loadFunc) {
        this(loadFunc, keyed -> nameFromId(keyed, ""), NamedCollection::keyedId);
    }

    public Stream<?> loadCollection(Server server) {
        if (!(this.loadFunc.apply(server) instanceof Stream<?> stream))
            throw new IllegalArgumentException("Invalid return type for named collection");
        return stream;
    }

    @SuppressWarnings("unchecked")
    public String getId(@NotNull Object o) {
        return ((Function<Object, String>) this.idGetter).apply(o);
    }

    @SuppressWarnings("unchecked")
    public String getName(@NotNull Object o) {
        return Objects.requireNonNullElseGet(((Function<Object, String>) this.nameGetter).apply(o), () -> nameFromId(o, getId(o)));
    }

    private static String nameFromId(@NotNull Object o, @NotNull String id) {
        if (!(o instanceof Keyed keyed)) return id;
        return keyed.key().value().replaceAll("_", " ");
    }

    private static <T extends Keyed> String keyedId(@NotNull T keyed) {
        return keyed.key().asString();
    }

    private static <T extends Keyed> @NotNull Stream<T> fromRegistry(RegistryKey<T> key, @Nullable Predicate<T> filter) {
        if (filter == null) filter = entry -> true;
        return RegistryAccess.registryAccess().getRegistry(key)
                .stream().filter(filter);
    }

    private static @NotNull String translatable(@NotNull Translatable translatable) {
        Component component = Component.translatable(translatable);
        Component output = GlobalTranslator.render(component, Locale.getDefault());
        return PlainTextComponentSerializer.plainText().serialize(output);
    }

    static {
        SERIALIZER = new PropertyLoadable.Serializer<>(
                String.class, NamedCollection.class,
                (str, issueHelper) -> {
                    try {
                        return valueOf(str.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        issueHelper.appendAtPath(WNC0.apply(str));
                        return NamedCollection.NONE;
                    }
                });
    }
}
