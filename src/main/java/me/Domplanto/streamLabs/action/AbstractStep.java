package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ConfigPathSegment(id = "step")
public abstract class AbstractStep<T> implements YamlPropertyObject {
    @SuppressWarnings("rawtypes")
    private final static Map<String, Class<? extends AbstractStep>> ACTIONS = ReflectUtil.loadClassesWithIds(AbstractStep.class);
    private final @NotNull Class<?> expectedDataType;
    private @Nullable StreamLabs plugin;

    public AbstractStep(@NotNull Class<T> expectedDataType) {
        this.expectedDataType = expectedDataType;
    }

    public static List<? extends AbstractStep<?>> parseAll(List<Map<String, Object>> sections, ConfigurationSection parent, ConfigIssueHelper issueHelper) {
        return sections.stream()
                .map(section -> {
                    issueHelper.push(AbstractStep.class, String.valueOf(sections.indexOf(section)));
                    AbstractStep<?> instance = deserialize(section, issueHelper, parent);
                    issueHelper.pop();
                    return instance;
                }).filter(Objects::nonNull).toList();
    }

    @SuppressWarnings("rawtypes")
    private static AbstractStep<?> deserialize(Map<String, Object> section, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (section == null) return null;

        try {
            Map<Object, Class<? extends AbstractStep>> steps = ACTIONS.entrySet().stream()
                    .filter(entry -> section.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(entry -> {
                        issueHelper.process(entry.getKey());
                        return section.get(entry.getKey());
                    }, Map.Entry::getValue));
            if (steps.size() > 1) {
                issueHelper.appendAtPath(WS1);
                return null;
            }

            Map.Entry<Object, Class<? extends AbstractStep>> stepData = steps.entrySet().stream().findFirst().orElse(null);
            if (stepData == null) {
                String key = section.keySet().stream().findFirst().orElse("");
                issueHelper.process(key);
                issueHelper.appendAtPath(WS0.apply(key));
                return null;
            }

            //noinspection unchecked
            AbstractStep<Object> step = ReflectUtil.instantiate(stepData.getValue(), AbstractStep.class);
            if (step == null)
                throw new RuntimeException("Failed to instantiate step instance, check the error mentioned above!");

            Object value = stepData.getKey();
            if (value != null && step.getOptionalDataSerializer() != null
                    && step.getOptionalDataSerializer().from().isAssignableFrom(value.getClass()))
                value = step.getOptionalDataSerializer().serializeObject(stepData.getKey());
            if (value == null || !step.getExpectedDataType().isAssignableFrom(value.getClass())) {
                issueHelper.appendAtPath(WS2(step.getExpectedDataType(), value));
                return null;
            }

            // "Hacky" solution to get a ConfigurationSection instance from the given map
            String id = "step-%s".formatted(UUID.randomUUID());
            step.acceptYamlProperties(parent.createSection(id, section), issueHelper);
            ConfigPathStack stack = issueHelper.stack();
            stack.get(stack.size() - 3).process(id);
            step.load(value, issueHelper);
            return step;
        } catch (Exception e) {
            issueHelper.appendAtPathAndLog(EI0, e);
            return null;
        }
    }

    public @Nullable Serializer<?, T> getOptionalDataSerializer() {
        return null;
    }

    public abstract void load(@NotNull T data, @NotNull ConfigIssueHelper issueHelper);

    public abstract void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException;

    public final void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) throws ActionFailureException {
        this.plugin = plugin;
        try {
            this.execute(ctx);
        } catch (Exception e) {
            throw new ActionFailureException("An unexpected internal error occurred", e);
        }
        this.plugin = null;
    }

    public void runOnServerThread(@NotNull Runnable action) {
        Bukkit.getScheduler().runTask(this.getPlugin(), action);
    }

    protected @NotNull StreamLabs getPlugin() {
        if (plugin == null) throw new IllegalStateException("Tried to access plugin outside of executor function");
        return this.plugin;
    }

    public @NotNull Class<?> getExpectedDataType() {
        return expectedDataType;
    }

    public record Serializer<F, T>(
            @NotNull Class<F> from,
            @NotNull Class<T> to,
            @NotNull Function<F, T> serializerFunc
    ) {
        public @Nullable T serializeObject(@Nullable Object input) {
            if (input == null || !from.isAssignableFrom(input.getClass())) return null;
            //noinspection unchecked
            return serialize((F) input);
        }

        public @Nullable T serialize(@NotNull F input) {
            return serializerFunc.apply(input);
        }
    }

    public static class ActionFailureException extends Exception {
        public ActionFailureException(String message) {
            super(message);
        }

        public ActionFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
