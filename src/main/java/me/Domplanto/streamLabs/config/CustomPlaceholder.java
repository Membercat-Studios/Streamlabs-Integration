package me.Domplanto.streamLabs.config;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CustomPlaceholder extends ActionPlaceholder {
    public CustomPlaceholder(@NotNull String id, @Nullable String defaultValue, List<StateBasedValue> values) {
        super(id, PlaceholderFunction.of((object, event) -> getValue(values, defaultValue, event, object)));
    }

    @NotNull
    private static String getValue(List<StateBasedValue> values, String defaultValue, StreamlabsEvent event, JsonObject object) {
        for (StateBasedValue value : values) {
            if (value.checkConditions(event, object))
                return value.value() != null ? value.value() : value.id();
        }

        return defaultValue != null ? defaultValue : "";
    }

    public record StateBasedValue(
            @NotNull String id,
            @Nullable String value,
            @Nullable List<String> conditionStrings,
            @Nullable List<String> donationConditionStrings
    ) {
        public boolean checkConditions(StreamlabsEvent event, JsonObject object) {
            ArrayList<Condition> conditionList = new ArrayList<>();
            if (this.conditionStrings() != null)
                conditionList.addAll(Condition.parseAll(this.conditionStrings(), event));
            if (event instanceof BasicDonationEvent donationEvent && this.donationConditionStrings() != null)
                conditionList.addAll(Condition.parseDonationConditions(this.donationConditionStrings(), donationEvent, object));

            for (Condition condition : conditionList)
                if (!condition.check(event, object)) return false;

            return true;
        }
    }
}
