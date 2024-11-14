package me.Domplanto.streamLabs.events.youtube;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class YoutubeFollowEvent extends StreamlabsEvent {
    public YoutubeFollowEvent() {
        super("youtube_subscription", "follow", StreamlabsPlatform.YOUTUBE);
    }

    @Override
    public @Nullable String getMessage(JsonObject object) {
        return String.format("%s%s subscribed on %sYouTube%s!", ChatColor.AQUA, getRelatedUser(object), ChatColor.RED, ChatColor.AQUA);
    }
}
