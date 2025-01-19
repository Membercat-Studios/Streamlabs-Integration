package me.Domplanto.streamLabs.events.youtube;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class YoutubeMembershipGiftEvent extends BasicDonationEvent {
    public YoutubeMembershipGiftEvent() {
        super("youtube_gift_memberships", "membershipGift", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("tier", object -> String.valueOf(object.get("getLevel").getAsInt()));
        this.addPlaceholder("tier_name", object -> object.get("levelName").getAsString());
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Memberships";
    }
}
