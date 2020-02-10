package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.easyway.AbstractTestUtils;
import com.rbkmoney.reporter.dao.ContractMetaDao;
import com.rbkmoney.reporter.service.StatisticService;
import com.rbkmoney.reporter.service.impl.PartyServiceImpl;
import com.rbkmoney.reporter.service.impl.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.service.impl.ProvisionOfServiceTemplateImpl;
import com.rbkmoney.reporter.service.impl.ReportCreatorServiceImpl;
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
                ReportCreatorServiceImpl.class,
                PartyServiceImpl.class,
                PaymentRegistryTemplateImpl.class,
                ProvisionOfServiceTemplateImpl.class,
        },
        initializers = AbstractTemplateConfig.Initializer.class
)
@TestPropertySource("classpath:application.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractTemplateConfig extends AbstractTestUtils {

    @MockBean
    protected StatisticService statisticService;

    @MockBean
    protected ContractMetaDao contractMetaDao;

    @MockBean
    protected PartyManagementSrv.Iface partyManagementClient;

    public static class Initializer extends ConfigFileApplicationContextInitializer {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            super.initialize(configurableApplicationContext);
        }
    }
}
