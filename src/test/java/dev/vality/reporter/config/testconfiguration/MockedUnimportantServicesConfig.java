package dev.vality.reporter.config.testconfiguration;

import dev.vality.reporter.service.ScheduleReports;
import dev.vality.reporter.service.StorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestConfiguration
public class MockedUnimportantServicesConfig {

    @MockBean
    private StorageService storageService;

    @MockBean
    private ScheduleReports scheduleReports;

}
