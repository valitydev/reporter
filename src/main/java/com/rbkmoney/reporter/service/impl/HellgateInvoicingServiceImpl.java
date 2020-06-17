package com.rbkmoney.reporter.service.impl;

import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.reporter.service.HellgateInvoicingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HellgateInvoicingServiceImpl implements HellgateInvoicingService {

    private final InvoicingSrv.Iface hgInvoicingService;

    private static final UserInfo USER_INFO = new UserInfo()
            .setId("admin")
            .setType(UserType.service_user(new ServiceUser()));

    @Override
    public Invoice getInvoice(String invoiceId, long sequenceId) throws Exception {
        return hgInvoicingService.get(USER_INFO, invoiceId, getEventRange((int) sequenceId));
    }

    private EventRange getEventRange(int sequenceId) {
        return new EventRange().setLimit(sequenceId);
    }

}
