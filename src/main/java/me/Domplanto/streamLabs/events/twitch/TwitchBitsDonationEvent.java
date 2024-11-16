package me.Domplanto.streamLabs.events.twitch;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class TwitchBitsDonationEvent extends BasicDonationEvent {
    public TwitchBitsDonationEvent() {
        super("twitch_bits", "bits", StreamlabsPlatform.TWITCH);
    }

    @Override
    public double calculateAmount(JsonObject object) {
        return object.get("amount").getAsLong();
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Bits";
    }
}
