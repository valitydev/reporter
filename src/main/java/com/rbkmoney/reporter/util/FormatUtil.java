package com.rbkmoney.reporter.util;

import java.math.BigDecimal;

public class FormatUtil {

    public static String formatCurrency(long value) {
        return BigDecimal.valueOf(value).movePointLeft(2).toString();
    }

}
