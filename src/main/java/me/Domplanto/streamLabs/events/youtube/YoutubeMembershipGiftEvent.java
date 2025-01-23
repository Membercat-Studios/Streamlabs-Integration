package me.Domplanto.streamLabs.events.youtube;

import com.google.gson.JsonObject;
import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.action.ActionExecutor;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;
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
    public boolean checkConditions(ActionExecutionContext ctx) {
        return calculateAmount(ctx.baseObject()) != -1 && super.checkConditions(ctx);
    }
}
