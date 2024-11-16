package me.Domplanto.streamLabs.events.streamlabs;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.jetbrains.annotations.NotNull;

public class BasicDonationEvent extends StreamlabsEvent {
    @SuppressWarnings("unused")
    public BasicDonationEvent() {
        super("streamlabs_donation", "donation", StreamlabsPlatform.STREAMLABS);
        this.addPlaceholders();
    }

    public BasicDonationEvent(String id, String apiName, StreamlabsPlatform platform) {
        super(id, apiName, platform);
        this.addPlaceholders();
    }

    private void addPlaceholders() {
        this.addPlaceholder("amount", object -> String.valueOf((int) calculateAmount(object)));
        this.addPlaceholder("amount_double", object -> String.format("%.2f", calculateAmount(object)));
        this.addPlaceholder("amount_formatted", object -> object.has("formattedAmount") ? object.get("formattedAmount").getAsString() : "");
        this.addPlaceholder("currency", this::getCurrency);
        this.addPlaceholder("message", object -> object.has("message") ? object.get("message").getAsString() : "");
    }

    public double calculateAmount(JsonObject object) {
        return object.get("amount").getAsDouble();
    }

    public @NotNull String getCurrency(JsonObject object) {
        return object.get("currency").getAsString();
    }
}
