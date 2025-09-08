package dev.vality.reporter.util;

import dev.vality.reporter.ReportRequest;
import dev.vality.reporter.ReportTimeRange;
import dev.vality.reporter.StatReportRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {

    @Test
    void testBuildToken() {
        String token = TokenUtil
                .buildToken(new ReportRequest("partyId", new ReportTimeRange("kek", "kek")), Arrays.asList("cancelled"),
                        "2016-03-22T06:12:27Z");
        assertNotNull(token);
    }

    @Test
    void testIsNotValid() {
        StatReportRequest statReportRequest = new StatReportRequest(new ReportRequest());
        statReportRequest.setContinuationToken("batToken");
        assertFalse(TokenUtil.isValid(statReportRequest));
    }

    @Test
    void testIsValid() {
        StatReportRequest statReportRequest = new StatReportRequest(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")));
        statReportRequest.setReportTypes(Arrays.asList("cancelled"));
        statReportRequest.setContinuationToken(TokenUtil.buildToken(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")),
                Arrays.asList("cancelled"), "2016-03-22T06:12:27Z"));
        assertTrue(TokenUtil.isValid(statReportRequest));
    }

    @Test
    void extractTime() {
        String expectedTime = TokenUtil.extractTime(TokenUtil.buildToken(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")),
                Arrays.asList("cancelled"), "2016-03-22T06:12:27Z"));
        assertEquals("2016-03-22T06:12:27Z", expectedTime);
    }
}
