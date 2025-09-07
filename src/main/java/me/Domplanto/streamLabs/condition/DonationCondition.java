package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.action.ActionExecutionContext;
import me.Domplanto.streamLabs.config.placeholder.ActionPlaceholder;
import me.Domplanto.streamLabs.config.issue.ConfigPathSegment;
import me.Domplanto.streamLabs.events.streamlabs.BasicDonationEvent;

@ConfigPathSegment(id = "donation_condition")
public class DonationCondition extends Condition {
    protected DonationCondition(Operator operator, boolean invert, ActionPlaceholder.PlaceholderFunction element1, ActionPlaceholder.PlaceholderFunction element2) {
        super(operator, invert, element1, element2);
    }

    @Override
    public boolean check(ActionExecutionContext ctx) {
        if (!(ctx.event() instanceof BasicDonationEvent donationEvent)) return false;
        String e1 = getElement1().execute(ctx.baseObject(), ctx);
        String e2 = getElement2().execute(ctx.baseObject(), ctx);
        if (!e1.equals(donationEvent.getCurrency(ctx.baseObject()))) return true;

        double amount = donationEvent.calculateAmount(ctx.baseObject());
        try {
            return isInverted() != getOperator().check(amount, Double.parseDouble(e2));
        } catch (NumberFormatException e) {
            return isInverted() != getOperator().check(String.valueOf(amount), e2);
        }
    }
}
