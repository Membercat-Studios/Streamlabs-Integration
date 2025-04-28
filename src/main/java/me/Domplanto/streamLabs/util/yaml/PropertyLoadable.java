package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public interface PropertyLoadable<T> extends YamlPropertyObject {
    @NotNull Class<T> getExpectedDataType();

    default @NotNull Set<Serializer<?, T>> getOptionalDataSerializers() {
        return Set.of();
    }

    void load(@NotNull T data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent);

    record Serializer<F, T>(
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
}
