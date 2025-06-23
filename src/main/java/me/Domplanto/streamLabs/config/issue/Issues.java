package me.Domplanto.streamLabs.config.issue;

import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.util.components.Translations;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.*;

public class Issues {
    public static ConfigIssue EL0 = new ConfigIssue("EL0", ConfigIssue.Level.WARNING);
    public static ConfigIssue EL1 = new ConfigIssue("EL1", ConfigIssue.Level.WARNING);
    public static Function<String, ConfigIssue> EL2 = errorMsg -> new ConfigIssue("EL2", ConfigIssue.Level.ERROR, newline().append(text(errorMsg)));
    public static ConfigIssue EL3 = new ConfigIssue("EL3", ConfigIssue.Level.ERROR);
    public static ConfigIssue EM0 = new ConfigIssue("EM0", ConfigIssue.Level.ERROR);
    public static BiFunction<Long, Long, ConfigIssue> EM1 = (config, current) -> new ConfigIssue("EM1", ConfigIssue.Level.ERROR, text(config), text(current));
    public static BiFunction<Long, Long, ConfigIssue> EM2 = (config, current) -> new ConfigIssue("EM2", ConfigIssue.Level.ERROR, text(config), text(current));

    public static ConfigIssue EI0 = new ConfigIssue("EI0", ConfigIssue.Level.ERROR);
    public static ConfigIssue EI1 = new ConfigIssue("EI1", ConfigIssue.Level.ERROR);
    public static ConfigIssue EI2 = new ConfigIssue("EI2", ConfigIssue.Level.ERROR);
    public static ConfigIssue ES0 = new ConfigIssue("ES0", ConfigIssue.Level.ERROR);

    public static ConfigIssue WI0 = new ConfigIssue("WI0", ConfigIssue.Level.WARNING);
    public static ConfigIssue WI1 = new ConfigIssue("WI1", ConfigIssue.Level.WARNING);
    public static Function<String, ConfigIssue> WA0 = action -> new ConfigIssue("WA0", ConfigIssue.Level.WARNING, text(action));
    public static ConfigIssue WA1 = new ConfigIssue("WA1", ConfigIssue.Level.WARNING);
    public static Function<String, ConfigIssue> WA2 = type -> new ConfigIssue("WA2", ConfigIssue.Level.WARNING, text(type));
    public static Function<String, ConfigIssue> WR0 = type -> new ConfigIssue("WR0", ConfigIssue.Level.WARNING, text(type));
    public static Function<String, ConfigIssue> WRE0 = mode -> new ConfigIssue("WRE0", ConfigIssue.Level.WARNING, text(mode));
    public static BiFunction<String, String, ConfigIssue> WM0 = (type, def) -> new ConfigIssue("WM0", ConfigIssue.Level.WARNING, text(type), text(def));
    public static ConfigIssue WM1 = new ConfigIssue("WM1", ConfigIssue.Level.WARNING, Translations.MINIMESSAGE_LINK);
    public static ConfigIssue WM2 = new ConfigIssue("WM2", ConfigIssue.Level.WARNING);
    public static BiFunction<String, ConditionGroup.Mode, ConfigIssue> WC0 = (modeString, groupMode) -> new ConfigIssue("WC0", ConfigIssue.Level.WARNING, text(modeString), text(groupMode.name()));
    public static ConfigIssue WC1 = new ConfigIssue("WC1", ConfigIssue.Level.WARNING);
    public static ConfigIssue WC2 = new ConfigIssue("WC2", ConfigIssue.Level.WARNING);
    public static BiFunction<String, String, ConfigIssue> WPI0 = (nameKey, stepId) -> new ConfigIssue("WPI0", ConfigIssue.Level.WARNING, translatable(nameKey), text(stepId));
    public static Function<String, ConfigIssue> WPI1 = nameKey -> new ConfigIssue("WPI1", ConfigIssue.Level.WARNING, translatable(nameKey));
    public static BiFunction<String, String, ConfigIssue> WPI3 = (nameKey, instead) -> new ConfigIssue("WPI3", ConfigIssue.Level.WARNING, translatable(nameKey), text(instead));
    public static ConfigIssue WRP0 = new ConfigIssue("WRP0", ConfigIssue.Level.WARNING);
    public static BiFunction<String, String, ConfigIssue> WD0 = (value, defaultVal) -> new ConfigIssue("WD0", ConfigIssue.Level.WARNING, text(value), text(defaultVal));
    public static Function<String, ConfigIssue> WE0 = expression -> new ConfigIssue("WE0", ConfigIssue.Level.WARNING, text(expression));
    public static Function<String, ConfigIssue> WE1 = action -> new ConfigIssue("WE1", ConfigIssue.Level.WARNING, text(action));
    public static Function<String, ConfigIssue> WE2 = group -> new ConfigIssue("WE2", ConfigIssue.Level.WARNING, text(group));
    public static ConfigIssue WE3 = new ConfigIssue("WE3", ConfigIssue.Level.WARNING);
    public static Function<String, ConfigIssue> WNC0 = extraction -> new ConfigIssue("WNC0", ConfigIssue.Level.WARNING, text(extraction));
    public static ConfigIssue WQ0 = new ConfigIssue("WQ0", ConfigIssue.Level.WARNING);
    public static ConfigIssue WNR0 = new ConfigIssue("WNR0", ConfigIssue.Level.WARNING);
    public static ConfigIssue WNR1 = new ConfigIssue("WNR1", ConfigIssue.Level.WARNING);
    public static ConfigIssue WNR2 = new ConfigIssue("WNR2", ConfigIssue.Level.WARNING);
    public static ConfigIssue WNR3 = new ConfigIssue("WNR3", ConfigIssue.Level.WARNING);
    public static ConfigIssue WQ1 = new ConfigIssue("WQ1", ConfigIssue.Level.WARNING);
    public static ConfigIssue WV0 = new ConfigIssue("WV0", ConfigIssue.Level.WARNING);
    public static Function<String, ConfigIssue> WY0 = yamlKey -> new ConfigIssue("WY0", ConfigIssue.Level.WARNING, text(yamlKey));

    public static ConfigIssue HCM0 = new ConfigIssue("HCM0", ConfigIssue.Level.HINT);
    public static ConfigIssue HR0 = new ConfigIssue("HR0", ConfigIssue.Level.HINT);
    public static ConfigIssue HB0 = new ConfigIssue("HB0", ConfigIssue.Level.HINT);
    public static ConfigIssue HC0 = new ConfigIssue("HC0", ConfigIssue.Level.HINT);
    public static ConfigIssue HC1 = new ConfigIssue("HC1", ConfigIssue.Level.HINT);
    public static ConfigIssue HCG0 = new ConfigIssue("HCG0", ConfigIssue.Level.HINT);
    public static ConfigIssue HCS0 = new ConfigIssue("HCS0", ConfigIssue.Level.HINT);
    public static ConfigIssue HI0 = new ConfigIssue("HI0", ConfigIssue.Level.HINT);

    public static ConfigIssue WI2(Field field, Object value, YamlPropertyObject object) throws ReflectiveOperationException {
        return new ConfigIssue("WI2", ConfigIssue.Level.WARNING, text(field.getType().getSimpleName()), text(value != null ? value.getClass().getSimpleName() : "null"), text(field.get(object).toString()));
    }

    public static ConfigIssue WPI2(String nameKey, @NotNull Class<?> expectedType, @Nullable Object actualObject) {
        String actualTypeName = Optional.ofNullable(actualObject).map(o -> o.getClass().getSimpleName()).orElse("null");
        return new ConfigIssue("WPI2", ConfigIssue.Level.WARNING, translatable(nameKey), text(expectedType.getSimpleName()), text(actualTypeName));
    }
}
