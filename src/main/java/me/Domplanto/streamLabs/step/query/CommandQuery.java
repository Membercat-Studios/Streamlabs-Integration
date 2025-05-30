package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyCustomDeserializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@ReflectUtil.ClassId("command")
public class CommandQuery extends AbstractQuery<String> {
    private static final String PLAYER_PLACEHOLDER = "\\{player\\}";
    private String command;
    @YamlProperty("output_format")
    private ComponentSerializer<Component, ?, ?> outputSerializer = PlainTextComponentSerializer.plainText();
    @YamlProperty("timeout")
    private Integer timeout = 500;
    @YamlProperty("context")
    private String context;
    @YamlProperty("cancel_on_invalid_context")
    private boolean cancelOnInvalidContext = false;

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.command = data;
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        String command = ActionPlaceholder.replacePlaceholders(this.command, ctx);
        CommandSender sender = Objects.requireNonNullElseGet(this.getSender(ctx, plugin), Bukkit::getConsoleSender);

        Set<String> affectedPlayers = ctx.config().getAffectedPlayers();
        if (!hasOutput()) {
            if (!Pattern.compile(PLAYER_PLACEHOLDER).matcher(command).find()) this.dispatch(command, sender, plugin);
            else affectedPlayers.forEach(player -> {
                String replacedCommand = command.replaceAll(PLAYER_PLACEHOLDER, player);
                this.dispatch(replacedCommand, sender, plugin);
            });
            return null;
        }

        String firstPlayer = affectedPlayers.stream().findFirst().orElse("");
        String replacedCommand = command.replaceAll(PLAYER_PLACEHOLDER, firstPlayer);
        return this.dispatchWithOutput(replacedCommand, plugin);
    }

    private void dispatch(@NotNull String command, CommandSender sender, StreamLabs plugin) {
        try {
            runOnServerThread(plugin, this.timeout, () -> Bukkit.dispatchCommand(sender, command));
        } catch (TimeoutException e) {
            StreamLabs.LOGGER.warning("Timeout while running command at %s, try manually specifying a higher timeout value!".formatted(location().toFormattedString()));
        }
    }

    private @Nullable String dispatchWithOutput(@NotNull String command, StreamLabs plugin) {
        CompletableFuture<Component> result = new CompletableFuture<>();
        CommandSender sender = Bukkit.createCommandSender(result::complete);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(sender, command));

        Component output;
        try {
            output = result.get(this.timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            output = Component.empty();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to execute command", e);
        }
        return outputSerializer.serialize(output).toString();
    }

    @YamlPropertyCustomDeserializer(propertyName = "output_format")
    private ComponentSerializer<?, ?, ?> deserializeOutputFormat(@Nullable String input, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (input == null) return this.outputSerializer;
        return Map.of(
                "text", PlainTextComponentSerializer.plainText(),
                "json", GsonComponentSerializer.gson(),
                "minimessage", MiniMessage.miniMessage(),
                "legacy_section", LegacyComponentSerializer.legacySection(),
                "legacy_ampersand", LegacyComponentSerializer.legacyAmpersand()
        ).get(input.toLowerCase());
    }

    private @Nullable CommandSender getSender(@NotNull ActionExecutionContext ctx, StreamLabs plugin) {
        if (this.context == null) return null;
        String selector = ActionPlaceholder.replacePlaceholders(this.context, ctx);
        Set<String> affectedPlayers = ctx.config().getAffectedPlayers();
        if (!affectedPlayers.isEmpty()) {
            String firstPlayer = affectedPlayers.stream().findFirst().orElseThrow();
            selector = selector.replaceAll(PLAYER_PLACEHOLDER, firstPlayer);
        }

        List<Entity> selected = plugin.getServer().selectEntities(Bukkit.getConsoleSender(), selector);
        if (selected.isEmpty()) {
            if (cancelOnInvalidContext) return null;
            StreamLabs.LOGGER.warning("No entity found for command context %s at %s, defaulting to console context!".formatted(this.context, location().toFormattedString()));
            return Bukkit.getConsoleSender();
        }

        if (!(selected.getFirst() instanceof CommandSender sender)) {
            if (cancelOnInvalidContext) return null;
            StreamLabs.LOGGER.warning("Selected entity for command context %s is not a valid command sender, at %s, defaulting to console context!".formatted(this.context, location().toFormattedString()));
            return Bukkit.getConsoleSender();
        }

        return sender;
    }

    @Override
    protected boolean isOptional() {
        return true;
    }

    @Override
    public @NotNull Class<String> getExpectedDataType() {
        return String.class;
    }
}
