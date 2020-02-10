package com.rbkmoney.reporter.config;

import com.rbkmoney.easyway.AbstractTestUtils;
import com.rbkmoney.reporter.handler.ReportsNewProtoHandler;
import com.rbkmoney.reporter.service.PartyService;
import com.rbkmoney.reporter.service.ReportNewProtoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {
                ReportsNewProtoHandler.class,
        },
        initializers = AbstractHandlerConfig.Initializer.class
)
@TestPropertySource("classpath:application.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractHandlerConfig extends AbstractTestUtils {

    @MockBean
    protected PartyService partyService;

    @MockBean
    protected ReportNewProtoService reportNewProtoService;

    public static class Initializer extends ConfigFileApplicationContextInitializer {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            super.initialize(configurableApplicationContext);
        }
    }
}
