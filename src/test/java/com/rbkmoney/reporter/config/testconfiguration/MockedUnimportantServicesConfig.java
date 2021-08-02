package com.rbkmoney.reporter.config.testconfiguration;

import com.rbkmoney.reporter.service.ScheduleReports;
import com.rbkmoney.reporter.service.StorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestConfiguration
public class MockedUnimportantServicesConfig {

    @MockBean
    private StorageService storageService;

    @MockBean
    private ScheduleReports scheduleReports;

}
