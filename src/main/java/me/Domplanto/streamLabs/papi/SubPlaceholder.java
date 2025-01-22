package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public abstract class SubPlaceholder {
    private final StreamLabs plugin;
    @NotNull
    private final String name;

    public SubPlaceholder(StreamLabs plugin, @NotNull String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public abstract @NotNull Optional<String> onRequest(OfflinePlayer player, @NotNull String params);

    public StreamLabs getPlugin() {
        return plugin;
    }

    public @NotNull String getName() {
        return name;
    }

    public static Set<? extends SubPlaceholder> findSubPlaceholderClasses(StreamLabs plugin) {
        return ReflectUtil.findClasses(SubPlaceholder.class, plugin);
    }
}
