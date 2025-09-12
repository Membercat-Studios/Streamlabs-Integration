package me.Domplanto.streamLabs.papi;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutor;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public class ExecutorSubPlaceholder extends SubPlaceholder {
    private final ActionExecutor executor;

    public ExecutorSubPlaceholder(StreamLabs plugin) {
        super(plugin, "executor");
        this.executor = plugin.getExecutor();
    }

    @Override
    public @NotNull Optional<Component> onRequest(OfflinePlayer player, @NotNull String params) {
        String[] additionalData = new String[0];
        if (params.contains(":")) {
            String[] data = params.split(":");
            params = data[0];
            if (data.length > 1) additionalData = ArrayUtils.subarray(data, 1, data.length + 1);
        }

        return Optional.of(String.valueOf(switch (params) {
            case "running_actions" -> executor.getRunningCount();
            case "running_instances" -> additionalData.length == 1 ? executor.getInstanceCount(additionalData[0])
                    : executor.getInstanceCount();
            case "queued_actions" -> executor.getQueuedCount();
            case "queued_instances" -> additionalData.length == 1 ? executor.getQueuedInstanceCount(additionalData[0])
                    : executor.getQueuedInstanceCount();
            case "global_queued_instances" -> executor.getGlobalQueuedCount();
            default -> "Unknown sub-placeholder \"%s\"".formatted(params);
        })).map(Component::text);
    }
}
