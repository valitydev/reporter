package com.rbkmoney.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatAdjustment {

    private String partyId;

    private String partyShopId;

    private String invoiceId;

    private String paymentId;

    private String adjustmentId;

    private Instant adjustmentStatusCreatedAt;

    private String adjustmentReason;

    private String adjustmentCurrencyCode;

    private Instant adjustmentCreatedAt;

    private Long adjustmentAmount;

}
