package com.membercat.streamlabs.events.youtube;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.events.StreamlabsPlatform;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;

@SuppressWarnings("unused")
public class YoutubeSuperchatEvent extends BasicDonationEvent {
    public YoutubeSuperchatEvent() {
        super("youtube_superchat", "superchat", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("amount_formatted", object -> object.has("displayString") ? object.get("displayString").getAsString() : "");
        this.addPlaceholder("message", object -> object.has("comment") ? object.get("comment").getAsString() : "");
    }

    @Override
    public double calculateAmount(JsonObject object) {
        return super.calculateAmount(object) / 1000000;
    }
}
