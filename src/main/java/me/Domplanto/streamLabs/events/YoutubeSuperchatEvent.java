package me.Domplanto.streamLabs.events;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class YoutubeSuperchatEvent extends BasicDonationEvent {
    public YoutubeSuperchatEvent() {
        super("youtube_superchat", "superchat", StreamlabsPlatform.YOUTUBE);
    }

    @Override
    public @Nullable String getMessage(JsonObject object) {
        return String.format("%s%s sent a superchat of %s%s!", ChatColor.RED, getRelatedUser(object), calculateAmount(object), getCurrency(object));
    }
}
