package com.membercat.streamlabs.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.components.Translations;
import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

@SuppressWarnings("UnstableApiUsage")
public abstract class SubCommand {
    private final StreamLabs pluginInstance;

    public SubCommand(StreamLabs pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public abstract LiteralCommandNode<CommandSourceStack> buildCommand();

    public final int exceptionHandler(CommandContext<CommandSourceStack> ctx, Consumer<CommandSender> action) {
        CommandSender sender = ctx.getSource().getSender();
        try {
            action.accept(sender);
        } catch (Exception e) {
            pluginInstance.getLogger().log(Level.SEVERE, "Unexpected error while trying to execute command", e);
            sender.sendMessage(Translations.withPrefix(Translations.UNEXPECTED_ERROR, true));
        }

        return Command.SINGLE_SUCCESS;
    }

    protected StreamLabs getPlugin() {
        return this.pluginInstance;
    }

    public static Set<? extends SubCommand> findSubCommandClasses(StreamLabs pluginInstance) {
        return ReflectUtil.initializeClasses(SubCommand.class, pluginInstance);
    }
}
