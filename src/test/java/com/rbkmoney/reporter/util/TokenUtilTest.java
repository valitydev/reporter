package com.rbkmoney.reporter.util;

import com.rbkmoney.reporter.ReportRequest;
import com.rbkmoney.reporter.ReportTimeRange;
import com.rbkmoney.reporter.StatReportRequest;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class TokenUtilTest {

    @Test
    public void testBuildToken() {
        String token = TokenUtil
                .buildToken(new ReportRequest("partyId", new ReportTimeRange("kek", "kek")), Arrays.asList("cancelled"),
                        "2016-03-22T06:12:27Z");
        assertNotNull(token);
    }

    @Test
    public void testIsNotValid() {
        StatReportRequest statReportRequest = new StatReportRequest(new ReportRequest());
        statReportRequest.setContinuationToken("batToken");
        assertFalse(TokenUtil.isValid(statReportRequest));
    }

    @Test
    public void testIsValid() {
        StatReportRequest statReportRequest = new StatReportRequest(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")));
        statReportRequest.setReportTypes(Arrays.asList("cancelled"));
        statReportRequest.setContinuationToken(TokenUtil.buildToken(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")),
                Arrays.asList("cancelled"), "2016-03-22T06:12:27Z"));
        assertTrue(TokenUtil.isValid(statReportRequest));
    }

    @Test
    public void extractTime() {
        String expectedTime = TokenUtil.extractTime(TokenUtil.buildToken(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")),
                Arrays.asList("cancelled"), "2016-03-22T06:12:27Z"));
        assertEquals("2016-03-22T06:12:27Z", expectedTime);
    }
}
