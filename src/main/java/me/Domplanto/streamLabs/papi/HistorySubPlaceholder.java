package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.statistics.EventHistory;
import me.Domplanto.streamLabs.statistics.EventHistorySelector;
import me.Domplanto.streamLabs.statistics.HistoryChangedListener;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
public class HistorySubPlaceholder extends SubPlaceholder implements HistoryChangedListener {
    private final EventHistory history;
    private final HashMap<String, EventHistorySelector> selectorCache = new HashMap<>();
    private final HashMap<String, String> values = new HashMap<>();

    public HistorySubPlaceholder(StreamLabs plugin) {
        super(plugin, "history");
        this.history = plugin.getExecutor().getEventHistory();
        this.history.registerListeners(this);
    }

    @Override
    public @NotNull Optional<String> onRequest(OfflinePlayer player, @NotNull String params) {
        if (!params.contains(":")) return Optional.of("Error: Missing section after history selector");
        int idx = params.indexOf(":");
        String selector = params.substring(0, idx);

        if (!selectorCache.containsKey(selector))
            this.buildSelector(selector);
        if (!values.containsKey(params)) {
            EventHistorySelector historySelector = selectorCache.get(selector);
            if (historySelector == null) return Optional.of("Error: Invalid history selector");
            EventHistory.LoggedEvent event = history.getEvent(historySelector);
            values.put(params, event != null ? event.replacePlaceholders(params.substring(idx + 1)) : "");
        }

        return Optional.of(this.values.get(params));
    }

    private void buildSelector(String selector) {
        ConfigIssueHelper issueHelper = new ConfigIssueHelper(null);
        EventHistorySelector built = EventHistorySelector.deserialize(selector, issueHelper);
        try {
            issueHelper.complete();
        } catch (ConfigLoadedWithIssuesException e) {
            if (e.getIssues().stream().anyMatch(issue -> issue.issue().getLevel() != ConfigIssue.Level.HINT))
                built = null;
        }

        this.selectorCache.put(selector, built);
    }

    @Override
    public void onHistoryChanged(EventHistory history, EventHistory.LoggedEvent newEvent) {
        this.values.clear();
    }
}
