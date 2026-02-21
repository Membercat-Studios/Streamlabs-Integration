package com.membercat.streamlabs.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import com.membercat.streamlabs.StreamLabs;
import com.membercat.streamlabs.command.argument.EventArgumentType;
import com.membercat.streamlabs.config.placeholder.ActionPlaceholder;
import com.membercat.streamlabs.events.StreamlabsEvent;
import com.membercat.streamlabs.events.youtube.YoutubeSuperchatEvent;
import com.membercat.streamlabs.util.components.Translations;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class TestSubCommand extends SubCommand {
    public TestSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return literal("test")
                .then(argument("event", EventArgumentType.event())
                        .then(placeholderArg(false))
                        .then(literal("bypassratelimiters")
                                .executes(ctx -> executeTest(ctx, true, Set.of()))
                                .then(placeholderArg(true)))
                        .executes(ctx -> executeTest(ctx, false, Set.of()))).build();
    }

    private CommandNode<CommandSourceStack> placeholderArg(boolean bypassRateLimiters) {
        //noinspection unchecked
        return argument("placeholders", new PlaceholderArgumentType("placeholders"))
                .executes(ctx -> executeTest(ctx, bypassRateLimiters, ctx.getArgument("placeholders", Set.class)))
                .build();
    }

    private int executeTest(CommandContext<CommandSourceStack> ctx, boolean bypassRateLimiters, Set<Pair<String, String>> placeholders) {
        return exceptionHandler(ctx, sender -> {
            StreamlabsEvent event = EventArgumentType.getEvent(ctx, "event");
            JsonObject object = new JsonObject();
            String user = "user%s".formatted(new Random().nextInt(10, 9999999));
            event.getPlaceholders().removeIf(pl -> !pl.name().startsWith("_"));
            event.addPlaceholder("user", o -> user);
            for (Pair<String, String> placeholder : placeholders) {
                event.addPlaceholder(placeholder.getKey(), o -> placeholder.getValue());
                try {
                    double value = Double.parseDouble(placeholder.getValue());
                    if (event instanceof YoutubeSuperchatEvent)
                        value = value * 1000000;

                    object.addProperty(placeholder.getKey(), value);
                } catch (NumberFormatException e) {
                    object.addProperty(placeholder.getKey(), placeholder.getValue());
                }
            }

            if (!getPlugin().getExecutor().checkAndExecute(event, object, bypassRateLimiters) && getPlugin().showStatusMessages())
                sender.sendMessage(Translations.withPrefix(Translations.ACTION_FAILURE, true));
        });
    }

    private record PlaceholderArgumentType(
            String ownName) implements CustomArgumentType<Set<Pair<String, String>>, String> {
        @Override
        public @NotNull Set<Pair<String, String>> parse(@NotNull StringReader reader) {
            String data = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());
            String[] elements = data.split(" ");
            return Arrays.stream(elements)
                    .map(PlaceholderArgumentType::parseOne)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        @Nullable
        private static Pair<String, String> parseOne(String element) {
            String[] data = element.split("=");
            if (data.length < 2)
                return null;

            return Pair.of(data[0], data[1]);
        }

        @Override
        public @NotNull ArgumentType<String> getNativeType() {
            return StringArgumentType.greedyString();
        }

        @Override
        public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, @NotNull SuggestionsBuilder builder) {
            ParsedCommandNode<?> lastNode = ctx.getNodes().getLast();
            String input = lastNode.getNode().getName().equals(ownName) ? lastNode.getRange().get(ctx.getInput()) : "";
            int elementStart = input.lastIndexOf(' ') + 1;
            String element = input.substring(elementStart);
            if (element.contains("=")) return builder.buildFuture();

            StreamlabsEvent event = EventArgumentType.getEvent(ctx, "event");
            event.getPlaceholders()
                    .stream().map(ActionPlaceholder::name)
                    .filter(name -> !name.startsWith("_") || element.startsWith("_"))
                    .map(name -> input.substring(0, elementStart) + name + "=")
                    .forEach(builder::suggest);
            return builder.buildFuture();
        }
    }
}