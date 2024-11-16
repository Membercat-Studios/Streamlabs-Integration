package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

import java.util.Set;

public abstract class SubCommand implements TabCompleter, CommandExecutor {
    private final StreamLabs pluginInstance;

    public SubCommand(StreamLabs pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public abstract String getName();

    protected StreamLabs getPlugin() {
        return this.pluginInstance;
    }

    public static Set<? extends SubCommand> findSubCommandClasses(StreamLabs pluginInstance) {
        return ReflectUtil.findClasses(SubCommand.class, pluginInstance);
    }
}
