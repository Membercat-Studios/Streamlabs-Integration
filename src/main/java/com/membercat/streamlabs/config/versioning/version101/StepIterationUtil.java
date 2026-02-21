package com.membercat.streamlabs.config.versioning.version101;

import com.membercat.streamlabs.step.AbstractLogicStep;
import com.membercat.streamlabs.util.ReflectUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.membercat.streamlabs.config.versioning.version100.StepsMigrator.getSubSections;

public final class StepIterationUtil {
    private static final Set<String> LOGIC_STEPS;

    public static void applyForStepResult(@NotNull ConfigurationSection root, @NotNull String stepId, Consumer<StepResult> action) {
        getAllSteps(root).stream()
                .filter(s -> s.step().containsKey(stepId))
                .forEach(action);
    }

    public static void applyForStep(@NotNull ConfigurationSection root, @NotNull String stepId, Consumer<Map<String, Object>> action) {
        applyForStepResult(root, stepId, s -> action.accept(s.step()));
    }

    public static <P> void applyForAndAfterStep(@NotNull ConfigurationSection root, @NotNull String stepId, Function<Map<String, Object>, P> action, BiConsumer<Map<String, Object>, @NotNull P> actionAfter) {
        applyForStepResult(root, stepId, result -> {
            P params = action.apply(result.step());
            if (params == null) return;
            StepResult.stream(result.parent().subList(result.stepIndex() + 1, result.parent().size()))
                    .flatMap(StepIterationUtil::withSubStepsRecursive).map(StepResult::step)
                    .forEach(step -> actionAfter.accept(step, params));
        });
    }

    public static void replaceStr(@NotNull Map<String, Object> step, Function<String, String> replacementFunction) {
        for (Map.Entry<String, Object> entry : step.entrySet()) {
            if (!(entry.getValue() instanceof String str)) continue;
            str = replacementFunction.apply(str);
            step.put(entry.getKey(), str);
        }
    }

    public static @NotNull List<StepResult> getAllSteps(@NotNull ConfigurationSection root) {
        List<StepResult> steps = new ArrayList<>(getStepsFrom(root, "actions"));
        steps.addAll(getStepsFrom(root, "goal_types"));
        steps.addAll(getStepsFrom(root, "functions"));
        return steps;
    }

    public static @NotNull List<StepResult> getStepsFrom(@NotNull ConfigurationSection root, @NotNull String sectionId) {
        ConfigurationSection section = root.getConfigurationSection(sectionId);
        if (section == null) return List.of();
        return getSubSections(section).stream()
                .flatMap(s -> StepResult.stream(s.get("steps")))
                .flatMap(StepIterationUtil::withSubStepsRecursive)
                .toList();
    }

    public static @NotNull Stream<StepResult> withSubStepsRecursive(@NotNull StepResult result) {
        Map<String, Object> step = result.step();
        Optional<String> key = LOGIC_STEPS.stream().filter(step::containsKey).findAny();
        if (key.isEmpty()) return Stream.of(result);
        Stream<StepResult> steps = StepResult.stream(step.get(key.get()));
        if (key.get().equals("check") && step.containsKey("else")) {
            steps = Stream.concat(steps, StepResult.stream(step.get("else")));
        }
        return Stream.concat(Stream.of(result), steps.flatMap(StepIterationUtil::withSubStepsRecursive));
    }

    static {
        LOGIC_STEPS = new HashSet<>(ReflectUtil.loadClassesWithIds(AbstractLogicStep.class, true).keySet());
        LOGIC_STEPS.add("check");
    }

    public record StepResult(
            @NotNull Map<String, Object> step,
            @NotNull List<? extends Map<String, Object>> parent,
            int stepIndex
    ) {
        public static @NotNull Stream<StepResult> stream(@Nullable Object steps) {
            if (steps == null) return Stream.of();
            try {
                //noinspection unchecked
                return stream((List<Map<String, Object>>) steps);
            } catch (Throwable e) {
                throw new IllegalArgumentException("Steps property is not a list");
            }
        }

        public static @NotNull Stream<StepResult> stream(@NotNull List<Map<String, Object>> steps) {
            return steps.stream().map(step -> new StepResult(step, steps, steps.indexOf(step)));
        }
    }
}
