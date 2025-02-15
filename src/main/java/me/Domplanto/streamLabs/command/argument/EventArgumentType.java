package me.Domplanto.streamLabs.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.command.exception.ComponentCommandExceptionType;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class EventArgumentType implements CustomArgumentType<StreamlabsEvent, String> {
    private static final ComponentCommandExceptionType EVENT_NOT_FOUND = new ComponentCommandExceptionType("streamlabs.commands.error.unknown_event_type", ColorScheme.INVALID);

    public static EventArgumentType event() {
        return new EventArgumentType();
    }

    public static StreamlabsEvent getEvent(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, StreamlabsEvent.class);
    }

    @Override
    public @NotNull StreamlabsEvent parse(StringReader stringReader) throws CommandSyntaxException {
        String id = stringReader.getString();
        //noinspection unchecked
        Optional<StreamlabsEvent> event = ((Set<StreamlabsEvent>) StreamLabs.getCachedEventObjects())
                .stream().filter(e -> e.getId().equals(id))
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
        StreamLabs.getCachedEventObjects().forEach(event -> builder.suggest(event.getId()));
        return builder.buildFuture();
    }
}
