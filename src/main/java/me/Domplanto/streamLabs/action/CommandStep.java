package me.Domplanto.streamLabs.action;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.BracketResolver;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@ReflectUtil.ClassId("command")
public class CommandStep extends AbstractStep<String> {
    private String command;
    @Nullable
    private String executionAmountExpression;
    @YamlProperty("context")
    private String context = "console";
    @YamlProperty("cancel_on_missing_context")
    private boolean cancelOnMissingContext = false;

    public CommandStep() {
        super(String.class);
    }

    public int calculateExecutionCount(ActionExecutionContext ctx) {
        if (this.executionAmountExpression == null) return 1;

        String fullExpression = ActionPlaceholder.replacePlaceholders(executionAmountExpression, ctx);
        try {
            return new DoubleEvaluator().evaluate(fullExpression).intValue();
        } catch (IllegalArgumentException e) {
            StreamLabs.LOGGER.warning("Failed to evaluate execution count expression \"%s\" (resolved from \"%s\") at %s, skipping command!"
                    .formatted(fullExpression, this.executionAmountExpression, getLocation().toFormattedString()));
            return 0;
        }
    }

    private @Nullable CommandSender getSender(@NotNull ActionExecutionContext ctx, StreamLabs plugin, ConfigPathStack location) {
        if ("console".equals(this.context)) return Bukkit.getConsoleSender();

        String selector = ActionPlaceholder.replacePlaceholders(this.context, ctx);
        List<Entity> selected = plugin.getServer().selectEntities(Bukkit.getConsoleSender(), selector);
        if (selected.isEmpty()) {
            if (cancelOnMissingContext) return null;
            StreamLabs.LOGGER.warning("No entity found for context %s at %s, defaulting to console context!".formatted(this.context, location.toFormattedString()));
            return Bukkit.getConsoleSender();
        }

        if (!(selected.getFirst() instanceof CommandSender sender)) {
            if (cancelOnMissingContext) return null;
            StreamLabs.LOGGER.warning("Selected entity for context %s is not a valid command sender, at %s, defaulting to console context!".formatted(this.context, location.toFormattedString()));
            return Bukkit.getConsoleSender();
        }

        return sender;
    }

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        BracketResolver resolver = new BracketResolver(data).resolve(issueHelper);
        this.command = resolver.getContent();
        this.executionAmountExpression = resolver.getBracketContents().orElse(null);
    }

    @Override
    public void execute(@NotNull ActionExecutionContext ctx) {
        StreamLabs plugin = getPlugin();
        ConfigPathStack location = getLocation();
        runOnServerThread(() -> {
            CommandSender sender = getSender(ctx, plugin, location);
            if (sender == null) return;
            String command = ActionPlaceholder.replacePlaceholders(this.command, ctx);
            Set<String> players = command.contains("{player}") ? ctx.config().getAffectedPlayers() : Set.of();
            for (int i = 0; i < calculateExecutionCount(ctx); i++) {
                if (players.isEmpty()) {
                    Bukkit.dispatchCommand(sender, command);
                    continue;
                }

                for (String player : players) {
                    String finalCommand = command.replace("{player}", player);
                    Bukkit.dispatchCommand(sender, finalCommand);
                }
            }
        });
    }
}
