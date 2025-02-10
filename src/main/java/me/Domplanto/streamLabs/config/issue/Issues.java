package me.Domplanto.streamLabs.config.issue;

import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.text;

public class Issues {
    public static ConfigIssue EI0 = new ConfigIssue("EI0", ConfigIssue.Level.ERROR);
    public static ConfigIssue EI1 = new ConfigIssue("EI1", ConfigIssue.Level.ERROR);
    public static ConfigIssue ES0 = new ConfigIssue("ES0", ConfigIssue.Level.ERROR);

    public static ConfigIssue WI0 = new ConfigIssue("WI0", ConfigIssue.Level.WARNING);
    public static ConfigIssue WI1 = new ConfigIssue("WI1", ConfigIssue.Level.WARNING);
    public static Function<String, ConfigIssue> WR0 = type -> new ConfigIssue("WR0", ConfigIssue.Level.WARNING, text(type));
    public static BiFunction<String, String, ConfigIssue> WM0 = (type, def) -> new ConfigIssue("WM0", ConfigIssue.Level.WARNING, text(type), text(def));
    public static ConfigIssue WM1 = new ConfigIssue("WM1", ConfigIssue.Level.WARNING);
    public static BiFunction<String, ConditionGroup.Mode, ConfigIssue> WC0 = (modeString, groupMode) -> new ConfigIssue("WC0", ConfigIssue.Level.WARNING, text(modeString), text(groupMode.name()));
    public static ConfigIssue WC1 = new ConfigIssue("WC1", ConfigIssue.Level.WARNING);
    public static ConfigIssue WC2 = new ConfigIssue("WC2", ConfigIssue.Level.WARNING);

    public static ConfigIssue HR0 = new ConfigIssue("HR0", ConfigIssue.Level.HINT);
    public static ConfigIssue HB0 = new ConfigIssue("HB0", ConfigIssue.Level.HINT);
    public static ConfigIssue HC0 = new ConfigIssue("HC0", ConfigIssue.Level.HINT);
    public static ConfigIssue HI0 = new ConfigIssue("HI0", ConfigIssue.Level.HINT);

    public static ConfigIssue WI2(Field field, Object value, YamlPropertyObject object) throws ReflectiveOperationException {
        return new ConfigIssue("WI2", ConfigIssue.Level.WARNING, text(field.getType().getSimpleName()), text(value != null ? value.getClass().getSimpleName() : "null"), text(field.get(object).toString()));
    }
}
