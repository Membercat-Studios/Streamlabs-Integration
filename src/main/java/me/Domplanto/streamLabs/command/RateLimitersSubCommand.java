package me.Domplanto.streamLabs.command;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static net.kyori.adventure.text.Component.text;

@SuppressWarnings("unused")
public class RateLimitersSubCommand extends SubCommand {
    public RateLimitersSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "ratelimiters";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length < 2 || !args[1].equals("reset")) {
            Translations.sendPrefixedResponse("streamlabs.command.error.invalid_sub_command", ColorScheme.INVALID, sender, text(args[0]));
            return true;
        }

        Collection<RateLimiter> rateLimiters = getPlugin().pluginConfig().fetchRateLimiters();
        rateLimiters.forEach(RateLimiter::reset);
        Translations.sendPrefixedResponse("streamlabs.commands.rate_limiters.reset", ColorScheme.DONE, sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 2)
            return List.of("reset");
        else return List.of();
    }
}
