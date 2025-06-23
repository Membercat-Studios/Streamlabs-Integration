package me.Domplanto.streamLabs.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ratelimiter.RateLimiter;
import me.Domplanto.streamLabs.util.components.ColorScheme;
import me.Domplanto.streamLabs.util.components.Translations;

import static io.papermc.paper.command.brigadier.Commands.literal;


@SuppressWarnings({"unused", "UnstableApiUsage"})
public class RateLimitersSubCommand extends SubCommand {
    public RateLimitersSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return literal("ratelimiters")
                .then(literal("reset")
                        .executes(ctx -> exceptionHandler(ctx, sender -> {
                            getPlugin().pluginConfig().fetchRateLimiters().forEach(RateLimiter::resetState);
                            Translations.sendPrefixedResponse("streamlabs.commands.rate_limiters.reset", ColorScheme.DONE, sender);
                        })))
                .build();
    }
}
