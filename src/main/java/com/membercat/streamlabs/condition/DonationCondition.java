package com.membercat.streamlabs.condition;

import com.membercat.streamlabs.action.ActionExecutionContext;
import com.membercat.streamlabs.config.placeholder.ActionPlaceholder;
import com.membercat.streamlabs.config.issue.ConfigPathSegment;
import com.membercat.streamlabs.events.streamlabs.BasicDonationEvent;

@ConfigPathSegment(id = "donation_condition")
public class DonationCondition extends Condition {
    protected DonationCondition(Operator operator, boolean invert, ActionPlaceholder.PlaceholderFunction element1, ActionPlaceholder.PlaceholderFunction element2) {
        super(operator, invert, element1, element2);
    }

    @Override
    public boolean check(ActionExecutionContext ctx) {
        if (!(ctx.event() instanceof BasicDonationEvent donationEvent)) return false;
        String e1 = getElement1().execute(ctx);
        String e2 = getElement2().execute(ctx);
        if (!e1.equals(donationEvent.getCurrency(ctx.baseObject()))) return false;

        double amount = donationEvent.calculateAmount(ctx.baseObject());
        try {
            return isInverted() != getOperator().check(amount, Double.parseDouble(e2));
        } catch (NumberFormatException e) {
            return isInverted() != getOperator().check(String.valueOf(amount), e2);
        }
    }
}
