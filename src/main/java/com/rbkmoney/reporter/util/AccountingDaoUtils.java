package com.rbkmoney.reporter.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jooq.Record1;

import java.math.BigDecimal;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccountingDaoUtils {

    public static Long getFundsAmountResult(Record1<BigDecimal> result) {
        return Optional.ofNullable(result)
                .map(r -> r.value1())
                .orElse(BigDecimal.ZERO)
                .longValue();
    }

    public static Long getFunds(BigDecimal value) {
        return Optional.ofNullable(value)
                .map(BigDecimal::longValue)
                .orElse(0L);
    }

}
