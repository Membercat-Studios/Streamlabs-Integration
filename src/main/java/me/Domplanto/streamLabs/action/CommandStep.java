package me.Domplanto.streamLabs.action;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@ReflectUtil.ClassId("command")
public class CommandStep extends AbstractStep<String> {
    private String command;
    @Nullable
    private String executionAmountExpression;

    public CommandStep() {
        super(String.class);
    }

    private void executeCommand(@NotNull String command) {
        runOnServerThread(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    public int calculateExecutionCount(ActionExecutionContext ctx) {
        if (this.executionAmountExpression == null) return 1;

        try {
            String fullExpression = ActionPlaceholder.replacePlaceholders(executionAmountExpression, ctx);
            return new DoubleEvaluator().evaluate(fullExpression).intValue();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper) {
        BracketResolver resolver = new BracketResolver(data).resolve(issueHelper);
        this.command = resolver.getContent();
        this.executionAmountExpression = resolver.getBracketContents().orElse(null);
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) {
        String command = ActionPlaceholder.replacePlaceholders(this.command, ctx);
        Set<String> players = command.contains("{player}") ? ctx.config().getAffectedPlayers() : Set.of();
        for (int i = 0; i < calculateExecutionCount(ctx); i++) {
            if (players.isEmpty()) {
                this.executeCommand(command);
                continue;
            }

            for (String player : players) {
                String finalCommand = command.replace("{player}", player);
                this.executeCommand(finalCommand);
            }
        }
    }
}
