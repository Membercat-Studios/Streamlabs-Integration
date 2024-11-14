package me.Domplanto.streamLabs.events.twitch;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TwitchFollowEvent extends StreamlabsEvent {
    public TwitchFollowEvent() {
        super("twitch_follow", "follow", StreamlabsPlatform.TWITCH);
    }

    @Override
    public @Nullable String getMessage(JsonObject object)  {
        return String.format("%s%s followed on %sTwitch%s!", ChatColor.AQUA, getRelatedUser(object), ChatColor.LIGHT_PURPLE, ChatColor.AQUA);
    }
}
