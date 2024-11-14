package me.Domplanto.streamLabs.events.youtube;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class YoutubeSuperchatEvent extends BasicDonationEvent {
    public YoutubeSuperchatEvent() {
        super("youtube_superchat", "superchat", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("amount_formatted", object -> object.has("displayString") ? object.get("displayString").getAsString() : "");
        this.addPlaceholder("message", object -> object.has("comment") ? object.get("comment").getAsString() : "");
    }

    @Override
    public @Nullable String getMessage(JsonObject object) {
        return String.format("%s%s sent a superchat of %s%s!", ChatColor.RED, getRelatedUser(object), calculateAmount(object), getCurrency(object));
    }
}
