package me.Domplanto.streamLabs.action.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.NamedCollection;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@ReflectUtil.ClassId("extract")
public class ExtractQuery extends TransformationQuery<NamedCollection> {
    private static final long TIMEOUT = 10000;
    private NamedCollection extractCollection;

    @Override
    public void load(@NotNull NamedCollection data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.extractCollection = data;
    }

    @Override
    protected String runQuery(@NotNull String input, @NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        try {
            String result = runOnServerThread(plugin, TIMEOUT, () -> this.extract(input, plugin.getServer()));
            return Optional.ofNullable(result).orElse("");
        } catch (TimeoutException e) {
            StreamLabs.LOGGER.warning("Extract query for type %s couldn't extract the correct value in time, action took >10000ms (tried to extract from: \"%s\")!".formatted(this.extractCollection, input));
            return "";
        }
    }

    private String extract(String input, Server server) {
        String lowerInput = input.toLowerCase();
        return this.extractCollection.loadCollection(server)
                .filter(e -> lowerInput.contains(extractCollection.getName(e).toLowerCase()))
                .findFirst().map(extractCollection::getId).orElse(null);
    }

    @Override
    public @NotNull Set<Serializer<?, NamedCollection>> getOptionalDataSerializers() {
        return Set.of(NamedCollection.SERIALIZER);
    }

    @Override
    public @NotNull Class<NamedCollection> getExpectedDataType() {
        return NamedCollection.class;
    }
}
