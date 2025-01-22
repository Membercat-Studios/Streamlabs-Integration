package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.statistics.EventHistory;
import me.Domplanto.streamLabs.statistics.EventHistorySelector;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public class HistorySubPlaceholder extends SubPlaceholder {
    private final EventHistory history;

    public HistorySubPlaceholder(StreamLabs plugin) {
        super(plugin, "history");
        this.history = plugin.getExecutor().getEventHistory();
    }

    @Override
    public @NotNull Optional<String> onRequest(OfflinePlayer player, @NotNull String params) {
        try {
            if (!params.contains(":")) return Optional.of(ChatColor.RED + "Missing section after history selector");
            int idx = params.indexOf(":");
            EventHistory.LoggedEvent event = history.getEvent(EventHistorySelector.deserialize(params.substring(0, idx + 1)));
            if (event == null) return Optional.empty();
            return Optional.of(event.replacePlaceholders(params.substring(idx + 1)));
        } catch (ConfigLoadedWithIssuesException e) {
            return Optional.of(ChatColor.RED + "Invalid history selector");
        }
    }
}
