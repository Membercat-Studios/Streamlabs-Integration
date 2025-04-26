package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutor;
import me.Domplanto.streamLabs.statistics.goal.DonationGoal;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public class GoalSubPlaceholder extends SubPlaceholder {
    private final ActionExecutor executor;

    public GoalSubPlaceholder(StreamLabs plugin) {
        super(plugin, "goal");
        this.executor = plugin.getExecutor();
    }

    @Override
    public @NotNull Optional<String> onRequest(OfflinePlayer player, @NotNull String params) {
        DonationGoal goal = this.executor.getActiveGoal();
        if (goal == null) return Optional.empty();
        return Optional.of(String.valueOf(switch (params) {
            case "state" -> goal.isActive() ? "active" : "stopped";
            case "amount" -> (int) goal.getValue();
            case "max" -> (int) goal.getGoal();
            case "percentage" -> (int) ((goal.getValue() / goal.getGoal()) * 100);
            default -> ChatColor.RED + "Unknown sub-placeholder \"%s\"".formatted(params);
        }));
    }
}
