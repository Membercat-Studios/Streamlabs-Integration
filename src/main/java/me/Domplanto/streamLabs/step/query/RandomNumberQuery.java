package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigLoadedWithIssuesException;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.config.placeholder.AbstractPlaceholder;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Random;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ReflectUtil.ClassId("random_number")
public class RandomNumberQuery extends AbstractQuery<String> {
    private NumRange range;
    private String rangeToParse;
    @YamlProperty("seed")
    private long seed;
    @YamlProperty("decimal")
    private int decimal;
    @YamlProperty("placeholders")
    private boolean placeholders;
    private Random random;

    @Override
    public void load(@NotNull String data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        if (!placeholders) this.range = NumRange.serializer().serialize(data, issueHelper);
        else this.rangeToParse = data;
        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        NumRange range = this.range;
        if (placeholders) {
            ComponentLogger logger = ComponentLogger.logger(RandomNumberQuery.class);
            ConfigIssueHelper issueHelper = new ConfigIssueHelper(logger);
            String rangeVal = AbstractPlaceholder.replacePlaceholders(rangeToParse, ctx);
            range = NumRange.serializer().serialize(rangeVal, issueHelper);
            try {
                issueHelper.complete();
            } catch (ConfigLoadedWithIssuesException e) {
                StreamLabs.LOGGER.warning("Failed to parse placeholder number range at %s (from %s)".formatted(location().toFormattedString(), rangeVal));
                return null;
            }
        }

        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(decimal);
        format.setRoundingMode(RoundingMode.DOWN);
        return format.format(Objects.requireNonNull(range).randomDouble(random));
    }

    @Override
    public @NotNull Class<String> getExpectedDataType() {
        return String.class;
    }

    @ConfigPathSegment(id = "Number Range")
    public record NumRange(
            double min,
            double max
    ) {
        private static final String SEPARATOR = "\\|";
        private static final NumRange DEFAULT = new NumRange(0, 1);

        public double randomDouble(Random random) {
            return random.nextDouble(min(), max());
        }

        public static Serializer<String, NumRange> serializer() {
            return new Serializer<>(
                    String.class, NumRange.class,
                    (str, issueHelper) -> {
                        String[] parts = str.split(SEPARATOR);
                        double max;
                        try {
                            if (parts.length <= 1) {
                                double result = Double.parseDouble(str);
                                if (result < 0) {
                                    issueHelper.appendAtPath(WNR3);
                                    return DEFAULT;
                                }
                                return new NumRange(0, result);
                            }
                            max = Double.parseDouble(parts[1]);
                        } catch (NumberFormatException e) {
                            issueHelper.appendAtPath(WNR1);
                            return DEFAULT;
                        }

                        try {
                            double min = Double.parseDouble(parts[0]);
                            if (max < min) {
                                issueHelper.appendAtPath(WNR2);
                            }
                            return new NumRange(min, max);
                        } catch (NumberFormatException e) {
                            issueHelper.appendAtPath(WNR0);
                            return DEFAULT;
                        }
                    });
        }
    }
}
