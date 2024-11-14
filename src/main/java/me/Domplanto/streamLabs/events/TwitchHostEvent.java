package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchHostEvent extends BasicDonationEvent {
    public TwitchHostEvent() {
        super("twitch_host", "host", StreamlabsPlatform.TWITCH);
    }

    public double calculateAmount(JsonObject object) {
        return object.get("viewers").getAsInt();
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Viewers";
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s hosted with %s viewers!", ChatColor.YELLOW, getRelatedUser(object), (int) calculateAmount(object));
    }
}
