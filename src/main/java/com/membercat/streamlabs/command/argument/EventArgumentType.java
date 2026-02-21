package com.membercat.streamlabs.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.command.exception.ComponentCommandExceptionType;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.util.ReflectUtil;
import com.membercat.streamlabs.util.components.ColorScheme;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class EventArgumentType implements CustomArgumentType<StreamlabsEvent, String> {
    private static final List<Class<StreamlabsEvent>> EVENT_CLASSES = ReflectUtil.loadClasses(StreamlabsEvent.class);
    private static final ComponentCommandExceptionType EVENT_NOT_FOUND = new ComponentCommandExceptionType("streamlabs.commands.error.unknown_event_type", ColorScheme.INVALID);

    public static EventArgumentType event() {
        return new EventArgumentType();
    }

    public static StreamlabsEvent getEvent(CommandContext<?> context, String name) {
        return context.getArgument(name, StreamlabsEvent.class);
    }

    @Override
    public @NotNull StreamlabsEvent parse(StringReader stringReader) throws CommandSyntaxException {
        String id = stringReader.readString();
        Optional<StreamlabsEvent> event = EVENT_CLASSES.stream()
                .map(cls -> ReflectUtil.instantiate(cls, StreamlabsEvent.class))
                .filter(Objects::nonNull).filter(e -> e.getId().equals(id))
                .findAny();
        if (event.isEmpty())
            throw EVENT_NOT_FOUND.create();

        return event.get();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
        StreamlabsIntegration.getCachedEventObjects().forEach(event -> builder.suggest(event.getId()));
        return builder.buildFuture();
    }
}
