package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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
                return NamedTextColor.RED + "Please specify a sub-placeholder!";
            int idx = params.indexOf("_");
            String param = params.substring(0, idx);
            String input = params.substring(idx + 1);
            return subPlaceholders.stream()
                    .filter(pl -> pl.getName().equals(param))
                    .findAny()
                    .map(pl -> pl.onRequest(player, input).orElse(""))
                    .orElse(NamedTextColor.RED + "Unknown sub-placeholder!");
        } catch (Exception e) {
            return NamedTextColor.RED + e.getClass().getSimpleName() + ": " + NamedTextColor.YELLOW + e.getMessage();
        }
    }
}
