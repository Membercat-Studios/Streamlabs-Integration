package me.Domplanto.streamLabs.events.youtube;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class YoutubeSubscriptionEvent extends StreamlabsEvent {
    public YoutubeSubscriptionEvent() {
        super("youtube_membership", "subscription", StreamlabsPlatform.YOUTUBE);
    }

    @Override
    public @Nullable String getMessage(JsonObject object) {
        return String.format("%s%s became a member!", ChatColor.DARK_GREEN, getRelatedUser(object));
    }
}
