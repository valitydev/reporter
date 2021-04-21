package com.rbkmoney.reporter.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jooq.Record1;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccountingDaoUtils {

    public static Long getFundsAmountResult(Record1<BigDecimal> result) {
        return result == null || result.value1() == null ? 0L : result.value1().longValue();
    }

    public static Long getFunds(BigDecimal value) {
        return value == null ? 0L : value.longValue();
    }

}
