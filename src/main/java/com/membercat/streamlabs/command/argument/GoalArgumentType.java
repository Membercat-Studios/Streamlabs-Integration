package com.membercat.streamlabs.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import com.membercat.streamlabs.command.exception.ComponentCommandExceptionType;
import com.membercat.streamlabs.config.PluginConfig;
import com.membercat.streamlabs.statistics.goal.DonationGoal;
import com.membercat.streamlabs.util.components.ColorScheme;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class GoalArgumentType implements CustomArgumentType<DonationGoal, String> {
    private static final ComponentCommandExceptionType GOAL_NOT_FOUND = new ComponentCommandExceptionType("streamlabs.commands.error.unknown_goal_type", ColorScheme.INVALID);
    private final PluginConfig config;

    private GoalArgumentType(PluginConfig config) {
        this.config = config;
    }

    public static GoalArgumentType goal(PluginConfig config) {
        return new GoalArgumentType(config);
    }

    public static DonationGoal getGoal(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, DonationGoal.class);
    }

    @Override
    public @NotNull DonationGoal parse(StringReader stringReader) throws CommandSyntaxException {
        DonationGoal goal = this.config.getGoal(stringReader.readString());
        if (goal == null)
            throw GOAL_NOT_FOUND.create();

        return goal;
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
        this.config.getGoals()
                .stream().map(goal -> goal.id)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
