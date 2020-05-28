package com.rbkmoney.reporter.handler.invoicing;

import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.reporter.handler.EventHandler;

public interface InvoicingEventHandler extends EventHandler<InvoiceChange> {

    UserInfo USER_INFO = new UserInfo()
            .setId("admin")
            .setType(UserType.service_user(new ServiceUser()));

    default EventRange getEventRange(int sequenceId) {
        return new EventRange().setLimit(sequenceId);
    }

}
