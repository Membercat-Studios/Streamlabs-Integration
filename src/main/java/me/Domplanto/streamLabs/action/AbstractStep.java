package me.Domplanto.streamLabs.action;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ConfigPathSegment(id = "step")
public abstract class AbstractStep<T> implements StepBase<T> {
    @SuppressWarnings("rawtypes")
    private final static Map<String, Class<? extends StepBase>> ACTIONS = ReflectUtil.loadClassesWithIds(StepBase.class);
    private final @NotNull Class<T> expectedDataType;
    private @Nullable StreamLabs plugin;
    private @Nullable ConfigPathStack configLocation;

    public AbstractStep(@NotNull Class<T> expectedDataType) {
        this.expectedDataType = expectedDataType;
    }

    public static List<? extends StepBase<?>> parseAll(List<Object> sections, ConfigurationSection parent, ConfigIssueHelper issueHelper) {
        //noinspection unchecked
        return sections.stream()
                .filter(obj -> {
                    boolean isMap = obj instanceof Map<?, ?>;
                    if (!isMap) {
                        issueHelper.push(AbstractStep.class, String.valueOf(sections.indexOf(obj)));
                        issueHelper.appendAtPath(WS3.apply(obj.toString()));
                        issueHelper.pop();
                    }
                    return isMap;
                })
                .map(map -> (Map<String, Object>) map)
                .flatMap(section -> {
                    issueHelper.push(AbstractStep.class, String.valueOf(sections.indexOf(section)));
                    Stream<? extends StepBase<?>> instance = Optional.ofNullable(deserialize(section, issueHelper, parent))
                            .map(Collection::stream).orElseGet(Stream::of);
                    issueHelper.pop();
                    return instance;
                }).filter(Objects::nonNull).toList();
    }

    @SuppressWarnings("rawtypes")
    private static List<? extends StepBase<?>> deserialize(Map<String, Object> section, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (section == null) return null;

        try {
            Map<String, Class<? extends StepBase>> steps = ACTIONS.entrySet().stream()
                    .filter(entry -> section.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (steps.size() > 1) {
                issueHelper.appendAtPath(WS1);
                return null;
            }

            Map.Entry<String, Class<? extends StepBase>> stepData = steps.entrySet().stream().findFirst().orElse(null);
            if (stepData == null) {
                String key = section.keySet().stream().findFirst().orElse("");
                issueHelper.process(key);
                issueHelper.appendAtPath(WS0.apply(key));
                return null;
            }

            Object dataVal = section.get(stepData.getKey());
            issueHelper.process(stepData.getKey());
            StepBase<Object> step = instantiate(stepData.getValue());
            if (!List.class.isAssignableFrom(step.getExpectedDataType()) && dataVal instanceof List<?> list) {
                issueHelper.pushProperty(stepData.getKey());
                List<StepBase<Object>> stepList = list.stream().map(obj -> {
                    issueHelper.push(AbstractStep.class, String.valueOf(list.indexOf(obj)));
                    Map<String, Object> map = new HashMap<>(section);
                    map.remove(stepData.getKey());
                    StepBase<Object> newStep = initializeSingle(stepData.getValue(), 2, map, null, obj, parent, issueHelper);
                    issueHelper.pop();
                    return newStep;
                }).toList();
                issueHelper.pop();
                return stepList;
            }

            List<StepBase<?>> stepList = new ArrayList<>();
            stepList.add(initializeSingle(stepData.getValue(), 0, section, stepData.getKey(), dataVal, parent, issueHelper));
            return stepList;
        } catch (Exception e) {
            issueHelper.appendAtPathAndLog(EI0, e);
            return null;
        }
    }

    private static StepBase<Object> initializeSingle(@SuppressWarnings("rawtypes") Class<? extends StepBase> stepCls, int stackOffset, Map<String, Object> section, @Nullable String key, Object value, ConfigurationSection parent, ConfigIssueHelper issueHelper) {
        StepBase<Object> step = instantiate(stepCls);
        if (key != null) issueHelper.pushProperty(key);
        if (value != null && step.getOptionalDataSerializer() != null
                && step.getOptionalDataSerializer().from().isAssignableFrom(value.getClass()))
            value = step.getOptionalDataSerializer().serializeObject(value);
        if (value == null || !step.getExpectedDataType().isAssignableFrom(value.getClass())) {
            issueHelper.appendAtPath(WS2(step.getExpectedDataType(), value));
            if (key != null) issueHelper.pop();
            return null;
        }
        if (key != null) issueHelper.pop();

        // "Hacky" solution to get a ConfigurationSection instance from the given map
        String id = "step-%s".formatted(UUID.randomUUID());
        ConfigurationSection newSection = parent.createSection(id, section);
        step.acceptYamlProperties(newSection, issueHelper);
        ConfigPathStack stack = issueHelper.stack();
        stack.get(stack.size() - (3 + stackOffset)).process(id);
        if (key != null) issueHelper.pushProperty(key);
        step.load(value, issueHelper, newSection);
        if (key != null) issueHelper.pop();
        return step;
    }

    private static StepBase<Object> instantiate(@SuppressWarnings("rawtypes") Class<? extends StepBase> stepCls) {
        //noinspection unchecked
        StepBase<Object> step = ReflectUtil.instantiate(stepCls, StepBase.class);
        if (step == null)
            throw new RuntimeException("Failed to instantiate step instance, check the error mentioned above!");
        return step;
    }

    @Override
    public void load(@NotNull T data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        this.configLocation = issueHelper.stackCopy();
    }

    protected abstract void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException;

    @Override
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

    protected @NotNull ConfigPathStack getLocation() {
        if (configLocation == null)
            throw new IllegalStateException("Tried to access config location outside of executor function");
        return this.configLocation;
    }

    @Override
    public @NotNull Class<T> getExpectedDataType() {
        return this.expectedDataType;
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
