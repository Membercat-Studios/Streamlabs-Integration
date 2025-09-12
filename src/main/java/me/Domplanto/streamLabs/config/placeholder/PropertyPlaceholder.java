package me.Domplanto.streamLabs.config.placeholder;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PropertyPlaceholder extends AbstractPlaceholder {
    private static final @RegExp String PROPERTY_PATTERN = "(?:\\.([a-zA-Z_]+))?";
    private final Pattern pattern;
    private final Map<String, String> properties;
    private @Nullable String defaultValue;

    public PropertyPlaceholder(@NotNull String name, @NotNull String format) throws PatternSyntaxException {
        super(name);
        this.properties = new HashMap<>();
        this.pattern = Pattern.compile(format.formatted(Pattern.quote(name()) + PROPERTY_PATTERN));
    }

    public @NotNull PropertyPlaceholder withDefaultValue(@Nullable String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyPlaceholder that)) return false;
        return Objects.equals(pattern.toString(), that.pattern.toString());
    }

    public void addProperty(@NotNull String key, @Nullable String value) {
        this.properties.put(key, value);
    }

    @Override
    public boolean isPresentIn(@NotNull String input) {
        return this.pattern.matcher(input).find();
    }

    @Override
    public @NotNull String replaceAll(@NotNull String input, ActionExecutionContext ctx) {
        return this.pattern.matcher(input).replaceAll(result -> {
            String key = result.group(1);
            if (key == null) {
                if (this.defaultValue == null) {
                    StreamLabs.LOGGER.warning("No property of placeholder \"%s\" specified, and no default value is present".formatted(name()));
                    return "";
                }
                return Matcher.quoteReplacement(this.defaultValue);
            }

            String value = this.properties.get(key);
            if (value == null)
                StreamLabs.LOGGER.warning("Attempted to resolve non-existent property \"%s\" of property placeholder \"%s\"".formatted(key, name()));
            return Matcher.quoteReplacement(Objects.requireNonNullElse(value, ""));
        });
    }
}
