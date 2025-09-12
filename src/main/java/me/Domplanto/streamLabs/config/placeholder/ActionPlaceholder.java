package me.Domplanto.streamLabs.config.placeholder;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionPlaceholder extends AbstractPlaceholder {
    private final PlaceholderFunction function;

    public ActionPlaceholder(@NotNull String name, PlaceholderFunction function) {
        super(name);
        this.function = function;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ActionPlaceholder placeholder)) return false;
        return this.name().equals(placeholder.name())
                && this.getFormat().equals(placeholder.getFormat());
    }

    public @NotNull String getFormat() {
        return "{%s}".formatted(name());
    }

    public PlaceholderFunction function() {
        return function;
    }

    @Override
    public boolean isPresentIn(@NotNull String input) {
        return input.contains(getFormat());
    }

    @Override
    public @NotNull String replaceAll(@NotNull String input, ActionExecutionContext ctx) {
        return input.replaceAll(Pattern.quote(getFormat()), Matcher.quoteReplacement(function().execute(ctx)));
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class PlaceholderFunction {
        @NotNull
        private final Function<ActionExecutionContext, String> valueFunction;

        private PlaceholderFunction(@NotNull Function<ActionExecutionContext, String> valueFunction) {
            this.valueFunction = valueFunction;
        }

        public static PlaceholderFunction of(@NotNull Object staticValue) {
            String string = Objects.requireNonNull(staticValue).toString();
            return new PlaceholderFunction(ctx -> string);
        }

        public static PlaceholderFunction ofObj(Function<JsonObject, String> valueFunction) {
            Objects.requireNonNull(valueFunction);
            return new PlaceholderFunction(ctx -> valueFunction.apply(ctx.baseObject()));
        }

        public static PlaceholderFunction of(Function<ActionExecutionContext, String> valueFunction) {
            return new PlaceholderFunction(Objects.requireNonNull(valueFunction));
        }

        public String execute(@Nullable ActionExecutionContext context) {
            String result = valueFunction.apply(context);
            return Objects.requireNonNullElse(result, "(Error while resolving placeholder)");
        }
    }
}
