package com.membercat.streamlabs.step;

import com.membercat.streamlabs.StreamlabsIntegration;
import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.config.issue.ConfigPathStack;
import com.membercat.streamlabs.util.yaml.PropertyBasedClassInitializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStep<T> implements StepBase<T> {
    @SuppressWarnings("rawtypes")
    public final static PropertyBasedClassInitializer<StepBase> INITIALIZER;
    public static final String NAME_ID = "step";

    static {
        MultiPropertyListSerializer serializer = new MultiPropertyListSerializer();
        INITIALIZER = new PropertyBasedClassInitializer<>(StepBase.class, true, NAME_ID, Set.of(serializer));
    }

    private final @NotNull Class<T> expectedDataType;
    private @Nullable StreamlabsIntegration plugin;
    private @Nullable ConfigPathStack configLocation;

    public AbstractStep(@NotNull Class<T> expectedDataType) {
        this.expectedDataType = expectedDataType;
    }

    @Override
    public void load(@NotNull T data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        this.configLocation = issueHelper.stackCopy();
    }

    protected abstract void execute(@NotNull ActionExecutionContext ctx) throws ActionFailureException;

    @Override
    public final void execute(@NotNull ActionExecutionContext ctx, @NotNull StreamlabsIntegration plugin) throws ActionFailureException {
        this.plugin = plugin;
        try {
            this.execute(ctx);
        } catch (Exception e) {
            throw new ActionFailureException("An unexpected internal error occurred", e);
        }
        this.plugin = null;
    }

    public void runOnServerThread(@NotNull Runnable action) {
        if (Bukkit.isPrimaryThread()) action.run();
        else Bukkit.getGlobalRegionScheduler().run(this.getPlugin(), task -> action.run());
    }

    protected @NotNull StreamlabsIntegration getPlugin() {
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

    @SuppressWarnings("rawtypes")
    private static class MultiPropertyListSerializer implements PropertyBasedClassInitializer.CustomSerializer<StepBase> {
        @Override
        public boolean shouldUse(@Nullable Object input, StepBase loadableInstance) {
            return input != null && List.class.isAssignableFrom(input.getClass())
                    && !List.class.isAssignableFrom(loadableInstance.getExpectedDataType());
        }

        @Override
        public @Nullable List<? extends StepBase> serialize(@Nullable Object input, Map.Entry<String, Class<? extends StepBase>> propertyData, PropertyBasedClassInitializer<StepBase> initializer, Map<String, Object> section, ConfigurationSection parent, ConfigIssueHelper issueHelper) {
            //noinspection unchecked
            List<Object> list = (List<Object>) input;
            assert list != null;
            return list.stream().map(obj -> {
                issueHelper.push(AbstractStep.class, String.valueOf(list.indexOf(obj)));
                Map<String, Object> map = new HashMap<>(section);
                map.remove(propertyData.getKey());
                StepBase newStep = initializer.initializeSingle(propertyData.getValue(), 2, map, null, obj, parent, issueHelper);
                issueHelper.pop();
                return newStep;
            }).toList();
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
