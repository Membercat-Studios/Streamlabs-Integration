package com.membercat.streamlabs.command.exception;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import com.membercat.streamlabs.util.components.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ComponentCommandExceptionType implements CommandExceptionType {
    private final Message message;

    public ComponentCommandExceptionType(String translationKey, TextColor color) {
        Component component = Translations.withPrefix(Component.translatable(translationKey, color), true);
        this.message = MessageComponentSerializer.message().serialize(component);
    }

    public CommandSyntaxException create() {
        return new CommandSyntaxException(this, message);
    }

    @Override
    public String toString() {
        return message.toString();
    }
}
