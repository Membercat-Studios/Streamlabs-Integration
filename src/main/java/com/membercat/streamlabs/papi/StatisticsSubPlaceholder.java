package com.membercat.streamlabs.papi;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.statistics.EventHistory;
import com.membercat.streamlabs.statistics.HistoryChangedListener;
import com.membercat.streamlabs.statistics.permanent.PermanentHistorySelector;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.text;

@SuppressWarnings("unused")
public class StatisticsSubPlaceholder extends SubPlaceholder implements HistoryChangedListener {
    private final EventHistory history;
    private final Map<String, PermanentHistorySelector> selectorCache = new HashMap<>();
    private final Map<String, String> cachedValues = new ConcurrentHashMap<>();

    public StatisticsSubPlaceholder(StreamlabsIntegration plugin) {
        super(plugin, "stats");
        this.history = plugin.getExecutor().getEventHistory();
        this.history.registerListeners(this);
    }

    @Override
    public @NotNull Optional<Component> onRequest(OfflinePlayer player, @NotNull String params) {
        PermanentHistorySelector selector = this.selectorCache.computeIfAbsent(params, this::deserializeSelector);
        if (selector != null && !this.cachedValues.containsKey(params)) this.asyncCacheUpdate(params, selector);
        return Optional.of(text(this.cachedValues.getOrDefault(params, "")));
    }

    private @Nullable PermanentHistorySelector deserializeSelector(@NotNull String params) {
        try {
            return PermanentHistorySelector.deserialize(params);
        } catch (IllegalArgumentException e) {
            this.cachedValues.put(params, e.getMessage());
            return null;
        }
    }

    @Override
    public void onHistoryChanged(EventHistory history, EventHistory.LoggedEvent newEvent) {
        this.cachedValues.clear();
    }

    private void asyncCacheUpdate(@NotNull String params, @NotNull PermanentHistorySelector selector) {
        Bukkit.getScheduler().runTaskAsynchronously(StreamlabsIntegration.getPlugin(StreamlabsIntegration.class), () -> {
            Integer result = this.history.queryPermanentSelector(selector);
            this.cachedValues.put(params, result != null ? result.toString() : "[DATABASE ERROR]");
        });
    }
}
