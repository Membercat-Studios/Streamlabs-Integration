package com.membercat.streamlabs.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;
import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class StatusSubCommand extends SubCommand {
    private static final Component CONNECTED = translatable()
            .key("streamlabs.status.connected")
            .color(ColorScheme.SUCCESS)
            .build();
    private static final Component DISCONNECTED = translatable()
            .key("streamlabs.status.disconnected")
            .color(ColorScheme.DISABLE)
            .build();

    public StatusSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("status")
                .executes(ctx -> exceptionHandler(ctx, sender -> {
                    Component status = translatable()
                            .key("streamlabs.commands.status.prefix")
                            .color(ColorScheme.STREAMLABS)
                            .append(text(" "))
                            .append(getPlugin().getSocketClient().isOpen() ? CONNECTED : DISCONNECTED)
                            .build();

                    sender.sendMessage(Translations.withPrefix(status, true));
                })).build();
    }
}
