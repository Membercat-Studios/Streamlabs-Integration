package me.Domplanto.streamLabs.events.twitch;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
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
