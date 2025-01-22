package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class RateLimitersCommand extends SubCommand {
    public RateLimitersCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "ratelimiters";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 2 || !args[1].equals("reset")) {
            sender.sendMessage(ChatColor.RED + "Unknown sub-command!");
            return true;
        }

        Collection<RateLimiter> rateLimiters = getPlugin().pluginConfig().fetchRateLimiters();
        rateLimiters.forEach(RateLimiter::reset);
        sender.sendMessage(ChatColor.AQUA + "Rate limiters have been successfully reset!");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 2)
            return List.of("reset");
        else return List.of();
    }
}
