package com.rbkmoney.reporter.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuartzJobUtil {

    public static JobKey buildJobKey(String partyId, String contractId, int calendarId, int scheduleId) {
        return JobKey.jobKey(
                String.format("job-%s-%s", partyId, contractId),
                buildGroupKey(calendarId, scheduleId)
        );
    }

    public static TriggerKey buildTriggerKey(String partyId, String contractId, int calendarId, int scheduleId,
                                             int triggerId) {
        return TriggerKey.triggerKey(
                String.format("trigger-%s-%s-%d", partyId, contractId, triggerId),
                buildGroupKey(calendarId, scheduleId)
        );
    }

    public static String buildGroupKey(int calendarId, int scheduleId) {
        return String.format("group-%d-%d", calendarId, scheduleId);
    }

}
