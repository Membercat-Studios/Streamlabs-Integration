package me.Domplanto.streamLabs.events.youtube;

import me.Domplanto.streamLabs.action.execution.ActionExecutionContext;
import me.Domplanto.streamLabs.events.StreamlabsEvent;
import me.Domplanto.streamLabs.events.StreamlabsPlatform;

@SuppressWarnings("unused")
public class YoutubeMembershipReceiveEvent extends StreamlabsEvent {
    public YoutubeMembershipReceiveEvent() {
        super("youtube_receive_membership", "membershipGift", StreamlabsPlatform.YOUTUBE);
        this.addPlaceholder("tier", object -> String.valueOf(object.get("membershipLevel").getAsInt()));
        this.addPlaceholder("tier_name", object -> object.get("levelName").getAsString());
        this.addContextPlaceholder("gifter", ctx -> ctx.executor().getEventHistory().getUserForMembershipId(ctx.baseObject().get("youtubeMembershipGiftId").getAsString()));
    }

    @Override
    public boolean checkConditions(ActionExecutionContext ctx) {
        return !ctx.baseObject().has("amount") && super.checkConditions(ctx);
    }
}
