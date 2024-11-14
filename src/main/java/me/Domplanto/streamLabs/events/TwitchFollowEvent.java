package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchFollowEvent extends BasicDonationEvent {
    public TwitchFollowEvent() {
        super("twitch_follow", "follow", StreamlabsPlatform.TWITCH);
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s followed on %sTwitch%s!", ChatColor.AQUA, getRelatedUser(object), ChatColor.LIGHT_PURPLE, ChatColor.AQUA);
    }
}
