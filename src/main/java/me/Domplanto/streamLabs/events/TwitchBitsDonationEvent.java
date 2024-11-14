package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchBitsDonationEvent extends BasicDonationEvent {
    public TwitchBitsDonationEvent() {
        super("twitch_bits", "bits", StreamlabsPlatform.TWITCH);
    }

    public double calculateAmount(JsonObject object) {
        return ((Long) object.get("amount").getAsLong()).doubleValue() / 100.0;
    }

    public @NotNull String getCurrency(JsonObject object) {
        return "Bits";
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s cheered %s bits!", ChatColor.DARK_PURPLE, getRelatedUser(object), calculateAmount(object));
    }
}
