package com.membercat.streamlabs.events.twitch;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.events.StreamlabsPlatform;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class TwitchRaidEvent extends BasicDonationEvent {
    public TwitchRaidEvent() {
        super("twitch_raid", "raid", StreamlabsPlatform.TWITCH);
    }

    @Override
    public double calculateAmount(JsonObject object) {
        return object.get("raiders").getAsInt();
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Viewers";
    }
}
