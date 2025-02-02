package me.Domplanto.streamLabs.action.command;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class ActionCommand {
    @NotNull
    private final String command;
    @Nullable
    private final String executionAmountExpression;

    private ActionCommand(@NotNull String command, @Nullable String executionAmountExpression) {
        this.command = command;
        this.executionAmountExpression = executionAmountExpression;
    }

    public void run(CommandSender commandSender, JavaPlugin plugin, ActionExecutionContext ctx) {
        String command = ActionPlaceholder.replacePlaceholders(this.command, ctx);
        Set<String> players = command.contains("{player}") ? ctx.config().getAffectedPlayers() : Set.of();
        for (int i = 0; i < calculateExecutionCount(ctx); i++) {
            for (String player : players) {
                String finalCommand = command.replace("{player}", player);
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(commandSender, finalCommand));
            }
        }
    }

    public int calculateExecutionCount(ActionExecutionContext ctx) {
        if (this.executionAmountExpression == null) return 1;

        String fullExpression = ActionPlaceholder.replacePlaceholders(executionAmountExpression, ctx);
        return new DoubleEvaluator().evaluate(fullExpression).intValue();
    }

    public static List<ActionCommand> parseAll(List<String> rawCommands, ConfigIssueHelper issueHelper) {
        return rawCommands.stream()
                .map(str -> {
                    issueHelper.push(ActionCommand.class, String.valueOf(rawCommands.indexOf(str)));
                    ActionCommand cmd = deserialize(str, issueHelper);
                    issueHelper.pop();
                    return cmd;
                })
                .toList();
    }

    private static ActionCommand deserialize(String input, ConfigIssueHelper issueHelper) {
        BracketResolver resolver = new BracketResolver(input).resolve(issueHelper);
        return new ActionCommand(resolver.getContent(), resolver.getBracketContents().orElse(null));
    }
}
