package com.rbkmoney.reporter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentFundsAmount {

    private Long fundsAcquiredAmount;
    private Long feeChargedAmount;

}
