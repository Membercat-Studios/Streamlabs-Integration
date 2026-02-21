package com.membercat.streamlabs.papi;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.config.issue.ConfigIssue;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigLoadedWithIssuesException;
import com.membercat.streamlabs.statistics.EventHistory;
import com.membercat.streamlabs.statistics.EventHistorySelector;
import com.membercat.streamlabs.statistics.HistoryChangedListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.kyori.adventure.text.Component.text;

@SuppressWarnings("unused")
public class HistorySubPlaceholder extends SubPlaceholder implements HistoryChangedListener {
    private final EventHistory history;
    private final HashMap<String, EventHistorySelector> selectorCache = new HashMap<>();
    private final HashMap<String, String> values = new HashMap<>();
    private final static Map<String, EventHistorySelector> DEFAULT_SELECTORS;

    public HistorySubPlaceholder(StreamLabs plugin) {
        super(plugin, "history");
        this.history = plugin.getExecutor().getEventHistory();
        this.history.registerListeners(this);
    }

    @Override
    public @NotNull Optional<Component> onRequest(OfflinePlayer player, @NotNull String params) {
        if (!params.contains(":")) return Optional.of(text("Error: Missing section after history selector"));
        int idx = params.indexOf(":");
        String selector = params.substring(0, idx);

        if (!selectorCache.containsKey(selector))
            this.selectorCache.put(selector, this.buildSelector(selector));
        if (!values.containsKey(params)) {
            EventHistorySelector historySelector = selectorCache.get(selector);
            if (historySelector == null) return Optional.of(text("Error: Invalid history selector"));
            EventHistory.LoggedEvent event = history.getEvent(historySelector);
            values.put(params, event != null ? event.replacePlaceholders(params.substring(idx + 1)) : "");
        }

        return Optional.of(text(this.values.get(params)));
    }

    @Nullable
    private EventHistorySelector buildSelector(String selector) {
        if (DEFAULT_SELECTORS.containsKey(selector))
            return DEFAULT_SELECTORS.get(selector);

        ConfigIssueHelper issueHelper = new ConfigIssueHelper(null);
        EventHistorySelector built = EventHistorySelector.deserialize(selector, issueHelper);
        try {
            issueHelper.complete();
        } catch (ConfigLoadedWithIssuesException e) {
            if (e.getIssues().stream().anyMatch(issue -> issue.issue().getLevel() != ConfigIssue.Level.HINT))
                built = null;
        }

        return built;
    }

    @Override
    public void onHistoryChanged(EventHistory history, EventHistory.LoggedEvent newEvent) {
        this.values.clear();
    }

    static {
        ComponentLogger logger = ComponentLogger.logger(HistorySubPlaceholder.class);
        ConfigIssueHelper issueHelper = new ConfigIssueHelper(logger);
        DEFAULT_SELECTORS = Map.of(
                "last_donation", EventHistorySelector.deserialize("last_[{amount}>0]", issueHelper),
                "last_follow", EventHistorySelector.deserialize("last_({_type}=youtube_subscription|{_type}=twitch_follow)", issueHelper)
        );

        try {
            issueHelper.complete();
        } catch (ConfigLoadedWithIssuesException e) {
            logger.error("Found issues while creating default history placeholders");
            logger.error(Component.newline().append(e.getIssues().getListMessage(-1, false)));
        }
    }
}
