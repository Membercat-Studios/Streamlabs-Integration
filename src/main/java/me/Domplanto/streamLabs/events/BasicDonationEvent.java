package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicDonationEvent extends StreamlabsEvent {
    @SuppressWarnings("unused")
    public BasicDonationEvent() {
        super("donation", StreamlabsPlatform.STREAMLABS);
    }

    public BasicDonationEvent(String id, StreamlabsPlatform platform) {
        super(id, platform);
    }

    public double calculateAmount(JsonObject object) {
        return object.get("amount").getAsDouble();
    }

    public @NotNull String getCurrency(JsonObject object) {
        return object.get("currency").getAsString();
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s donated %s%s!", ChatColor.GREEN, getRelatedUser(object), calculateAmount(object), getCurrency(object));
    }

    @Override
    public boolean checkThreshold(JsonObject object, double threshold) {
        return calculateAmount(object) >= threshold;
    }
}
