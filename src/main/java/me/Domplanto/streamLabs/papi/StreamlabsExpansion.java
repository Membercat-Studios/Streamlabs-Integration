package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static net.kyori.adventure.text.Component.text;

public class StreamlabsExpansion extends PlaceholderExpansion {
    private final Set<? extends SubPlaceholder> subPlaceholders;
    private final StreamLabs plugin;

    public StreamlabsExpansion(StreamLabs plugin) {
        this.plugin = plugin;
        this.subPlaceholders = SubPlaceholder.findSubPlaceholderClasses(plugin);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "streamlabs";
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        try {
            if (!params.contains("_"))
                return legacy(text("Please specify a sub-placeholder!", NamedTextColor.RED));
            int idx = params.indexOf("_");
            String param = params.substring(0, idx);
            String input = params.substring(idx + 1);
            return subPlaceholders.stream()
                    .filter(pl -> pl.getName().equals(param))
                    .findAny()
                    .map(pl -> pl.onRequest(player, input).map(StreamlabsExpansion::legacy).orElse(""))
                    .orElse(legacy(text("Unknown sub-placeholder!", NamedTextColor.RED)));
        } catch (Exception e) {
            return legacy(text(e.getClass().getSimpleName() + ": ", NamedTextColor.RED).append(text(e.getMessage(), NamedTextColor.YELLOW)));
        }
    }

    private static String legacy(@NotNull Component input) {
        return LegacyComponentSerializer.legacySection().serialize(input);
    }
}
