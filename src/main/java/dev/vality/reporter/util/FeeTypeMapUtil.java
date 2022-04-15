package dev.vality.reporter.util;

import dev.vality.reporter.model.FeeType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FeeTypeMapUtil {

    public static boolean isContainsAnyFee(Map<FeeType, Long> fees) {
        return fees.keySet().stream()
                .anyMatch(
                        feeType -> List.of(FeeType.FEE, FeeType.PROVIDER_FEE, FeeType.EXTERNAL_FEE).contains(feeType));
    }

    public static boolean isContainsAmount(Map<FeeType, Long> fees) {
        return fees.keySet().stream()
                .anyMatch(feeType -> Objects.equals(FeeType.AMOUNT, feeType));
    }
}
