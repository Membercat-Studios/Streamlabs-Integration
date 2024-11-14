package me.Domplanto.streamLabs.events.streamlabs;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicDonationEvent extends StreamlabsEvent {
    @SuppressWarnings("unused")
    public BasicDonationEvent() {
        super("streamlabs_donation", "donation", StreamlabsPlatform.STREAMLABS);
    }

    public BasicDonationEvent(String id, String apiName, StreamlabsPlatform platform) {
        super(id, apiName, platform);
    }

    public double calculateAmount(JsonObject object) {
        return object.get("amount").getAsDouble();
    }

    public @NotNull String getCurrency(JsonObject object) {
        return object.get("currency").getAsString();
    }

    @Override
    public @Nullable String getMessage(JsonObject object) {
        return String.format("%s%s donated %s%s!", ChatColor.GREEN, getRelatedUser(object), calculateAmount(object), getCurrency(object));
    }

    @Override
    public boolean checkThreshold(JsonObject object, double threshold) {
        return calculateAmount(object) >= threshold;
    }
}
