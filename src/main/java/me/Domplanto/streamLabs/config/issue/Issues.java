package me.Domplanto.streamLabs.config.issue;

import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.util.yaml.YamlPropertyObject;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Issues {
    public static ConfigIssue EI0 = new ConfigIssue("EI0", ConfigIssue.Level.ERROR, "Internal error during deserialization");
    public static ConfigIssue EI1 = new ConfigIssue("EI1", ConfigIssue.Level.ERROR, "Error in an internal configuration system, please report the exception found in the logs to the developers!");
    public static ConfigIssue ES0 = new ConfigIssue("ES0", ConfigIssue.Level.ERROR, "Your socket token has not been configured yet, make sure to follow our guide on setting up this plugin!");

    public static ConfigIssue WI0 = new ConfigIssue("WI0", ConfigIssue.Level.WARNING, "Failed to deserialize property, detailed information can be found in the logs!");
    public static ConfigIssue WI1 = new ConfigIssue("WI1", ConfigIssue.Level.WARNING, "Failed to assign issues to property, detailed information can be found in the logs!");
    public static Function<String, ConfigIssue> WR0 = type -> new ConfigIssue("WR0", ConfigIssue.Level.WARNING, "No rate limiter of type \"%s\" could be found, possible typo?".formatted(type));
    public static BiFunction<String, String, ConfigIssue> WM0 = (type, def) -> new ConfigIssue("WM0", ConfigIssue.Level.WARNING, "No message of type \"%s\" could be found, possible typo? (Defaulting to %s)".formatted(type, def));
    public static ConfigIssue WM1 = new ConfigIssue("WM1", ConfigIssue.Level.WARNING, "Message contains legacy formatting codes (&), which are not supported by MiniMessage. MiniMessage tags will not be processed in this message, for more info take a look at https://docs.advntr.dev/minimessage/");
    public static BiFunction<String, ConditionGroup.Mode, ConfigIssue> WC0 = (modeString, groupMode) -> new ConfigIssue("WC0", ConfigIssue.Level.WARNING, "No condition group mode \"%s\" could be found, defaulting to %s".formatted(modeString, groupMode));
    public static ConfigIssue WC1 = new ConfigIssue("WC1", ConfigIssue.Level.WARNING, "No valid condition operator found, skipping condition");
    public static ConfigIssue WC2 = new ConfigIssue("WC2", ConfigIssue.Level.WARNING, "Condition contains no elements and will be skipped");

    public static ConfigIssue HR0 = new ConfigIssue("HR0", ConfigIssue.Level.HINT, "The value of the rate limiter was implicitly set to empty, since it is not directly specified in the config. Make sure to explicitly set value to empty in the config to dismiss this hint and avoid accidentally configuring your rate limiter wrong!");
    public static ConfigIssue HB0 = new ConfigIssue("HB0", ConfigIssue.Level.HINT, "Element contains a non-terminated bracket at the start");
    public static ConfigIssue HC0 = new ConfigIssue("HC0", ConfigIssue.Level.HINT, "Condition element contains a non-terminated placeholder");
    public static ConfigIssue HI0 = new ConfigIssue("HI0", ConfigIssue.Level.HINT, "Debug mode should ONLY be used for development or to help with reporting issues, it will spam your console with Streamlabs API data!");

    public static ConfigIssue WI2(Field field, Object value, YamlPropertyObject object) throws ReflectiveOperationException {
        return new ConfigIssue("WI2", ConfigIssue.Level.WARNING, "Unexpected property type found, expected %s but got %s (now using default \"%s\")"
                .formatted(field.getType().getSimpleName(), value != null ? value.getClass().getSimpleName() : "null", field.get(object)));
    }
}
