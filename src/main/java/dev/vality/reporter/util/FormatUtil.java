package dev.vality.reporter.util;

import java.math.BigDecimal;

public class FormatUtil {

    public static String formatCurrency(long value, short exponent) {
        return BigDecimal.valueOf(value).movePointLeft(exponent).toPlainString();
    }

}
