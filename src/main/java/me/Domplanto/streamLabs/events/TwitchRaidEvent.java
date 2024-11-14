package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchRaidEvent extends BasicDonationEvent {
    public TwitchRaidEvent() {
        super("twitch_raid", "raid", StreamlabsPlatform.TWITCH);
    }

    public double calculateAmount(JsonObject object) {
        return object.get("raiders").getAsInt();
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Viewers";
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s raided with %s viewers!", ChatColor.GOLD, getRelatedUser(object), (int) calculateAmount(object));
    }
}
