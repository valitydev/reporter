package dev.vality.reporter.util;

import dev.vality.reporter.ReportRequest;
import dev.vality.reporter.ReportTimeRange;
import dev.vality.reporter.StatReportRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {

    @Test
    void testBuildToken() {
        String token = TokenUtil
                .buildToken(new ReportRequest("partyId", new ReportTimeRange("kek", "kek")), Arrays.asList("cancelled"),
                        "2016-03-22T06:12:27Z");
        String json = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        assertEquals("{\"partyId\":\"partyId\",\"shopId\":null,\"fromTime\":\"kek\",\"toTime\":\"kek\"," +
                "\"reportTypes\":[\"cancelled\"],\"createdAfter\":\"2016-03-22T06:12:27Z\"}", json);
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
    void testIsValidForExistingToken() {
        String json = "{\"partyId\":\"partyId\",\"shopId\":null," +
                "\"fromTime\":\"2016-03-22T05:12:27Z\",\"toTime\":\"2016-03-22T06:12:27Z\"," +
                "\"reportTypes\":[\"cancelled\"],\"createdAfter\":\"2016-03-22T06:12:27Z\"}";
        var request = new StatReportRequest(new ReportRequest("partyId",
                new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")))
                .setReportTypes(Arrays.asList("cancelled"))
                .setContinuationToken(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));

        assertEquals("2016-03-22T06:12:27Z", TokenUtil.extractTime(request.getContinuationToken()));
        assertTrue(TokenUtil.isValid(request));
    }

    @Test
    void extractTime() {
        String expectedTime = TokenUtil.extractTime(TokenUtil.buildToken(
                new ReportRequest("partyId", new ReportTimeRange("2016-03-22T05:12:27Z", "2016-03-22T06:12:27Z")),
                Arrays.asList("cancelled"), "2016-03-22T06:12:27Z"));
        assertEquals("2016-03-22T06:12:27Z", expectedTime);
    }
}
