package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.kyori.adventure.text.Component.text;

@SuppressWarnings("unused")
public class GoalSubCommand extends SubCommand {
    public GoalSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "goal";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            Translations.sendPrefixedResponse("streamlabs.command.error.missing_sub_command", ColorScheme.INVALID, sender);
            return true;
        }

        switch (args[1]) {
            case "start" -> {
                DonationGoal goal = getPlugin().pluginConfig().getGoal(args[2]);
                if (goal == null) {
                    Translations.sendPrefixedResponse("streamlabs.commands.goal.error_not_found", ColorScheme.INVALID, sender, text(args[2]));
                    return true;
                }
                if (args.length < 4) {
                    Translations.sendPrefixedResponse("streamlabs.commands.goal.error_no_amount", ColorScheme.INVALID, sender);
                    return true;
                }
                try {
                    getPlugin().getExecutor().activateGoal(goal, Integer.parseInt(args[3]));
                } catch (NumberFormatException e) {
                    Translations.sendPrefixedResponse("streamlabs.commands.goal.error_amount_invalid", ColorScheme.INVALID, sender);
                    return true;
                }

                Translations.sendPrefixedResponse("streamlabs.commands.goal.goal_started", ColorScheme.SUCCESS, sender, text(args[2]));
            }
            case "stop" -> {
                if (getPlugin().getExecutor().getActiveGoal() == null) {
                    Translations.sendPrefixedResponse("streamlabs.commands.goal.error_no_goal_active", ColorScheme.DISABLE, sender);
                    return true;
                }

                getPlugin().getExecutor().stopGoal();
                Translations.sendPrefixedResponse("streamlabs.commands.goal.goal_stopped", ColorScheme.SUCCESS, sender);
            }
            case "remove" -> {
                if (getPlugin().getExecutor().getActiveGoal() == null) {
                    Translations.sendPrefixedResponse("streamlabs.commands.goal.error_no_goal_active", ColorScheme.DISABLE, sender);
                    return true;
                }

                getPlugin().getExecutor().removeGoal();
                Translations.sendPrefixedResponse("streamlabs.commands.goal.goal_removed", ColorScheme.SUCCESS, sender);
            }
            default -> Translations.sendPrefixedResponse("streamlabs.command.error.invalid_sub_command", ColorScheme.INVALID, sender, text(args[1]));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 2)
            return List.of("start", "stop", "remove");
        if (args[1].equals("start")) {
            if (args.length == 3)
                return getPlugin().pluginConfig().getGoals()
                        .stream().map(goal -> goal.id)
                        .toList();
            if (args.length == 4)
                return List.of("<goal>");
        }
        return null;
    }
}
