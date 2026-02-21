package com.membercat.streamlabs.papi;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.util.ReflectUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public abstract class SubPlaceholder {
    private final StreamlabsIntegration plugin;
    @NotNull
    private final String name;

    public SubPlaceholder(StreamlabsIntegration plugin, @NotNull String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public abstract @NotNull Optional<Component> onRequest(OfflinePlayer player, @NotNull String params);

    public StreamlabsIntegration getPlugin() {
        return plugin;
    }

    public @NotNull String getName() {
        return name;
    }

    public static Set<? extends SubPlaceholder> findSubPlaceholderClasses(StreamlabsIntegration plugin) {
        return ReflectUtil.initializeClasses(SubPlaceholder.class, plugin);
    }
}
