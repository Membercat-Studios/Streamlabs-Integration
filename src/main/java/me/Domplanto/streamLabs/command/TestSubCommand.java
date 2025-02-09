package me.Domplanto.streamLabs.command;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.youtube.YoutubeSuperchatEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unused")
public class TestSubCommand extends SubCommand {
    public TestSubCommand(StreamLabs pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Please specify an event type");
            return true;
        }

        StreamlabsEvent event = StreamlabsEvent.findEventClasses().stream()
                .filter(e -> e.getId().equals(args[1]))
                .findFirst()
                .orElse(null);
        if (event == null) {
            sender.sendMessage(ChatColor.RED + "Unknown event type!");
            return true;
        }

        JsonObject object = new JsonObject();
        String user = "user%s".formatted(new Random().nextInt(10, 9999999));
        event.getPlaceholders().removeIf(pl -> !pl.name().startsWith("_"));
        event.addPlaceholder("user", o -> user);
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("bypassratelimiters")) {
                event.bypassRateLimiters();
                continue;
            }
            if (!arg.contains("=")) continue;

            String[] data = arg.split("=");
            if (data[0].length() <= 1) continue;

            String value2 = data.length > 1 ? data[1] : "";
            event.addPlaceholder(data[0], o -> value2);
            try {
                double value = Double.parseDouble(value2);
                if (event instanceof YoutubeSuperchatEvent)
                    value = value * 1000000;

                object.addProperty(data[0], value);
            } catch (NumberFormatException e) {
                object.addProperty(data[0], value2);
            }
        }

        if (!getPlugin().getExecutor().checkAndExecute(event, object))
            sender.sendMessage(ChatColor.RED + "Failed to execute some actions, check the logs for more details!");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 2)
            return StreamLabs.getCachedEventObjects()
                    .stream().map(StreamlabsEvent::getId)
                    .filter(id -> id.contains(args[1]))
                    .toList();

        StreamlabsEvent event = StreamLabs.getCachedEventObjects()
                .stream().filter(e -> e.getId().equals(args[1]))
                .findFirst().orElse(null);
        if (event == null) return List.of();
        if (!args[args.length - 1].contains("=")) {
            ArrayList<String> placeholders = new ArrayList<>(event.getPlaceholders()
                    .stream().map(ActionPlaceholder::name)
                    .filter(name -> !name.startsWith("_") || args[args.length - 1].startsWith("_"))
                    .toList());
            if (!Arrays.stream(args).toList().contains("bypassratelimiters"))
                placeholders.add("bypassratelimiters");
            return placeholders;
        }

        return null;
    }
}
