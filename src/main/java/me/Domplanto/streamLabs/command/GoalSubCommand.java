package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Invalid command arguments!");
            return true;
        }

        switch (args[1]) {
            case "start" -> {
                DonationGoal goal = getPlugin().pluginConfig().getGoal(args[2]);
                if (goal == null) {
                    sender.sendMessage(ChatColor.RED + String.format("No goal type \"%s\" was found.", args[2]));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Please enter a goal amount!");
                    return true;
                }
                try {
                    getPlugin().getExecutor().activateGoal(goal, Integer.parseInt(args[3]));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Goal amount is not a valid number!");
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + String.format("A goal of type %s has been started!", args[2]));
            }
            case "stop" -> {
                if (getPlugin().getExecutor().getActiveGoal() == null) {
                    sender.sendMessage(ChatColor.RED + "No goal is currently active!");
                    return true;
                }

                getPlugin().getExecutor().stopGoal();
                sender.sendMessage(ChatColor.GREEN + "The current goal has been stopped!");
            }
            case "remove" -> {
                if (getPlugin().getExecutor().getActiveGoal() == null) {
                    sender.sendMessage(ChatColor.RED + "No goal is currently active!");
                    return true;
                }

                getPlugin().getExecutor().removeGoal();
                sender.sendMessage(ChatColor.GREEN + "Goal has been removed!");
            }
            default -> sender.sendMessage(ChatColor.RED + String.format("Unknown sub-command \"%s\"", args[1]));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
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
