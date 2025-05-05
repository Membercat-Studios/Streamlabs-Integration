package me.Domplanto.streamLabs.action.query;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.Keyed;
import org.bukkit.Server;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static me.Domplanto.streamLabs.config.issue.Issues.WEX1;

@ReflectUtil.ClassId("extract")
public class ExtractQuery extends TransformationQuery<ExtractQuery.ExtractionType> {
    private static final long TIMEOUT = 10000;
    private ExtractionType extractionType;

    @Override
    public void load(@NotNull ExtractionType data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.extractionType = data;
    }

    @Override
    protected String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        try {
            String result = runOnServerThread(plugin, TIMEOUT, () -> this.extractionType.extract(input, plugin.getServer()));
            return Optional.ofNullable(result).orElse("");
        } catch (TimeoutException e) {
            StreamLabs.LOGGER.warning("Extract query for type %s couldn't extract the correct value in time, action took >10000ms (tried to extract from: \"%s\")!".formatted(this.extractionType, input));
            return "";
        }
    }

    @Override
    public @NotNull Set<Serializer<?, ExtractionType>> getOptionalDataSerializers() {
        return Set.of(new Serializer<>(
                String.class, ExtractionType.class,
                (str, issueHelper) -> {
                    try {
                        return ExtractionType.valueOf(str.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        issueHelper.appendAtPath(WEX1.apply(str));
                        return ExtractionType.NONE;
                    }
                }));
    }

    @Override
    public @NotNull Class<ExtractionType> getExpectedDataType() {
        return ExtractionType.class;
    }

    @SuppressWarnings("UnstableApiUsage")
    public enum ExtractionType {
        NONE((s, server) -> null),
        PLAYER((s, server) -> server.getOnlinePlayers().stream()
                .map(Player::getName).filter(player -> s.toLowerCase().contains(player.toLowerCase()))
                .findFirst().orElse(null)),
        ENTITY_TYPE((s, server) -> fromRegistry(RegistryKey.ENTITY_TYPE, s, EntityType::isSpawnable)),
        LIVING_ENTITY_TYPE((s, server) -> fromRegistry(RegistryKey.ENTITY_TYPE, s, type -> type.isSpawnable() && type.isAlive())),
        ENCHANTMENT((s, server) -> fromRegistry(RegistryKey.ENCHANTMENT, s, null)),
        ITEM((s, server) -> fromRegistry(RegistryKey.ITEM, s, null)),
        BLOCK((s, server) -> fromRegistry(RegistryKey.BLOCK, s, null)),
        NON_AIR_BLOCK((s, server) -> fromRegistry(RegistryKey.BLOCK, s, Predicate.not(BlockType::isAir))),
        BIOME((s, server) -> fromRegistry(RegistryKey.BIOME, s, null)),
        STRUCTURE((s, server) -> fromRegistry(RegistryKey.STRUCTURE, s, null));
        private final BiFunction<String, Server, String> extractionFunc;

        ExtractionType(BiFunction<String, Server, String> extractionFunc) {
            this.extractionFunc = extractionFunc;
        }

        private static <T extends Keyed> @Nullable String fromRegistry(RegistryKey<T> key, String s, @Nullable Predicate<T> filter) {
            if (filter == null) filter = entry -> true;
            return RegistryAccess.registryAccess().getRegistry(key)
                    .stream().filter(filter)
                    .filter(entry -> s.toLowerCase().contains(entry.key().value().toLowerCase().replaceAll("_", " ")))
                    .findFirst().map(entry -> entry.getKey().asString()).orElse(null);
        }

        public @Nullable String extract(@NotNull String input, @NotNull Server server) {
            return this.extractionFunc.apply(input, server);
        }
    }
}
