package com.membercat.streamlabs.papi;

import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.util.ReflectUtil;
import net.kyori.adventure.text.Component;
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

    public abstract @NotNull Optional<Component> onRequest(OfflinePlayer player, @NotNull String params);

    public StreamLabs getPlugin() {
        return plugin;
    }

    public @NotNull String getName() {
        return name;
    }

    public static Set<? extends SubPlaceholder> findSubPlaceholderClasses(StreamLabs plugin) {
        return ReflectUtil.initializeClasses(SubPlaceholder.class, plugin);
    }
}
