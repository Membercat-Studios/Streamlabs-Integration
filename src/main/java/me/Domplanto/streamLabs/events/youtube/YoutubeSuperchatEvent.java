package me.Domplanto.streamLabs.events.youtube;

import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

@SuppressWarnings("unused")
public class YoutubeSuperchatEvent extends BasicDonationEvent {
    public YoutubeSuperchatEvent() {
        super("youtube_superchat", "superchat", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("amount_formatted", object -> object.has("displayString") ? object.get("displayString").getAsString() : "");
        this.addPlaceholder("message", object -> object.has("comment") ? object.get("comment").getAsString() : "");
    }
}
