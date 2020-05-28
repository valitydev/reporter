package com.rbkmoney.reporter.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.rbkmoney.reporter.util.FeeType.*;

public class FeeTypeMapUtil {

    public static boolean isContainsAnyFee(Map<FeeType, Long> fees) {
        return fees.keySet().stream()
                .anyMatch(feeType -> List.of(FEE, PROVIDER_FEE, EXTERNAL_FEE).contains(feeType));
    }

    public static boolean isContainsAmount(Map<FeeType, Long> fees) {
        return fees.keySet().stream()
                .anyMatch(feeType -> Objects.equals(AMOUNT, feeType));
    }
}
