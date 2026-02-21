package com.membercat.streamlabs.command;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.membercat.streamlabs.action.ratelimiter.RateLimiter;
import com.membercat.streamlabs.util.components.ColorScheme;
import com.membercat.streamlabs.util.components.Translations;

import static io.papermc.paper.command.brigadier.Commands.literal;


@SuppressWarnings({"unused"})
public class RateLimitersSubCommand extends SubCommand {
    public RateLimitersSubCommand(StreamlabsIntegration pluginInstance) {
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
