package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
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
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        try {
            if (!params.contains("_"))
                return ChatColor.RED + "Please specify a sub-placeholder!";
            int idx = params.indexOf("_");
            String param = params.substring(0, idx);
            String input = params.substring(idx + 1);
            return subPlaceholders.stream()
                    .filter(pl -> pl.getName().equals(param))
                    .findAny()
                    .map(pl -> pl.onRequest(player, input).orElse(""))
                    .orElse(ChatColor.RED + "Unknown sub-placeholder!");
        } catch (Exception e) {
            return ChatColor.RED + e.getClass().getSimpleName() + ": " + ChatColor.YELLOW + e.getMessage();
        }
    }
}
