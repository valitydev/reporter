package com.rbkmoney.reporter.handler.payout;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.PayoutChange;

public abstract class AbstractPayoutHandler {

    public abstract void handle(PayoutChange change, Event event);

    public abstract boolean accept(PayoutChange change);

}
