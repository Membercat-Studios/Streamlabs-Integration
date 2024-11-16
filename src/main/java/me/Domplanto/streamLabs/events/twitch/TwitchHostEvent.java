package me.Domplanto.streamLabs.events.twitch;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class TwitchHostEvent extends BasicDonationEvent {
    public TwitchHostEvent() {
        super("twitch_host", "host", StreamlabsPlatform.TWITCH);
    }

    @Override
    public double calculateAmount(JsonObject object) {
        return object.get("viewers").getAsInt();
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Viewers";
    }
}
