package com.rbkmoney.reporter;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static com.rbkmoney.reporter.util.TimeUtil.toZoneSameLocal;
import static org.junit.Assert.assertEquals;

public class TimeUtilTest {

    @Test
    public void testToZoneSameLocal() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");

        Instant instant = Instant.now();
        Instant instant2 = toZoneSameLocal(instant, zoneId);
        int offsetSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
        assertEquals(offsetSeconds, Duration.between(instant, instant2).getSeconds());
    }

}
