package com.membercat.streamlabs.events.youtube;

import com.google.gson.JsonObject;
import com.membercat.streamlabs.action.ActionExecutor;
import com.membercat.streamlabs.events.StreamlabsPlatform;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class YoutubeMembershipGiftEvent extends BasicDonationEvent {
    public YoutubeMembershipGiftEvent() {
        super("youtube_gift_memberships", "membershipGift", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("tier", object -> String.valueOf(object.get("giftMembershipsLevel").getAsInt()));
        this.addPlaceholder("tier_name", object -> object.get("levelName").getAsString());
    }

    @Override
    public void onExecute(ActionExecutor executor, JsonObject baseObject) {
        if (!baseObject.has("membershipMessageId") || baseObject.get("membershipMessageId").isJsonNull()) return;
        executor.getEventHistory().storeGiftedMembershipId(baseObject.get("membershipMessageId").getAsString(), this.getRelatedUser(baseObject));
    }

    @Override
    public @NotNull String getCurrency(JsonObject object) {
        return "Memberships";
    }

    @Override
    public boolean isEventValid(@NotNull JsonObject baseObject) {
        return calculateAmount(baseObject) != -1;
    }
}
