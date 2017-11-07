package com.rbkmoney.reporter.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TimeUtil {

    public static Instant toZoneSameLocal(Instant instant, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(instant, zoneId).withZoneSameLocal(ZoneOffset.UTC).toInstant();
    }

}
