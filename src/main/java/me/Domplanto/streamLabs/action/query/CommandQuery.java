package me.Domplanto.streamLabs.action.query;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@ReflectUtil.ClassId("command")
public class CommandQuery extends AbstractQuery<String> {
    private String command;
    @YamlProperty("output_format")
    private ComponentSerializer<Component, ?, ?> outputSerializer = PlainTextComponentSerializer.plainText();
    @YamlProperty("timeout")
    private Integer timeout = 500;

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.command = data;
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        String command = ActionPlaceholder.replacePlaceholders(this.command, ctx);

        AtomicReference<Component> result = new AtomicReference<>();
        CommandSender sender = Bukkit.createCommandSender(result::set);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(sender, command));
        long time = System.currentTimeMillis();
        while (result.get() == null) {
            if ((time + this.timeout) < System.currentTimeMillis()) return null;
        }

        return outputSerializer.serialize(result.get()).toString();
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

    @Override
    public @NotNull Class<String> getExpectedDataType() {
        return String.class;
    }
}
