package me.Domplanto.streamLabs.step.query;

import me.Domplanto.streamLabs.StreamLabs;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.util.ReflectUtil;
import me.Domplanto.streamLabs.util.yaml.YamlProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Set;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@ReflectUtil.ClassId("random_number")
public class RandomNumberQuery extends AbstractQuery<RandomNumberQuery.NumRange> {
    private NumRange range;
    @YamlProperty("seed")
    private long seed;
    @YamlProperty("decimal")
    private int decimal;
    private Random random;

    @Override
    public void load(@NotNull NumRange data, @NotNull ConfigIssueHelper issueHelper, @NotNull ConfigurationSection parent) {
        super.load(data, issueHelper, parent);
        this.range = data;
        this.random = seed != 0 ? new Random(seed) : new Random();
    }

    @Override
    protected @Nullable String runQuery(@NotNull ActionExecutionContext ctx, @NotNull StreamLabs plugin) {
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(decimal);
        format.setRoundingMode(RoundingMode.DOWN);
        return format.format(range.randomDouble(random));
    }

    @Override
    public @NotNull Set<Serializer<?, NumRange>> getOptionalDataSerializers() {
        return Set.of(NumRange.serializer(), NumRange.intSerializer());
    }

    @Override
    public @NotNull Class<NumRange> getExpectedDataType() {
        return NumRange.class;
    }

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

        public static Serializer<Integer, NumRange> intSerializer() {
            return new Serializer<>(Integer.class, NumRange.class,
                    (i, issueHelper) -> {
                        if (i < 1) {
                            issueHelper.appendAtPath(WNR3);
                            return DEFAULT;
                        }
                        return new NumRange(0, i);
                    });
        }
    }
}
