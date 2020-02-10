package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.merch_stat.StatPayment;
import com.rbkmoney.damsel.merch_stat.StatRefund;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.easyway.AbstractTestUtils;
import com.rbkmoney.easyway.TestContainers;
import com.rbkmoney.easyway.TestContainersBuilder;
import com.rbkmoney.easyway.TestContainersParameters;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.reporter.dao.impl.ContractMetaDaoImpl;
import com.rbkmoney.reporter.dao.impl.ReportDaoImpl;
import com.rbkmoney.reporter.domain.tables.pojos.FileMeta;
import com.rbkmoney.reporter.job.GenerateReportJob;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.*;
import com.rbkmoney.reporter.service.impl.PartyServiceImpl;
import com.rbkmoney.reporter.service.impl.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.service.impl.ProvisionOfServiceTemplateImpl;
import com.rbkmoney.reporter.service.impl.TaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.FailureDetectingExternalResource;

import java.net.URL;
import java.time.Instant;
import java.util.Iterator;
import java.util.function.Supplier;

import static com.rbkmoney.reporter.utils.TestDataUtil.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@EnableScheduling
@ContextConfiguration(
        classes = {
                DataSourceAutoConfiguration.class,
                JdbcTemplateAutoConfiguration.class,
                TransactionAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                QuartzAutoConfiguration.class,
                ApplicationConfig.class,
                ReportDaoImpl.class,
                ContractMetaDaoImpl.class,
                PartyServiceImpl.class,
                ReportService.class,
                GenerateReportJob.class,
                DomainConfigService.class,
                TaskServiceImpl.class,
        },
        initializers = AbstractSchedulerConfig.Initializer.class
)
@TestPropertySource("classpath:application.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractSchedulerConfig extends AbstractTestUtils {

    @MockBean
    protected StatisticService statisticService;

    @MockBean
    protected SignService signService;

    @MockBean
    protected StorageService storageService;

    @MockBean
    protected EventPublisher eventPublisher;

    @MockBean
    protected PaymentRegistryTemplateImpl paymentRegistryTemplate;

    @MockBean
    protected ProvisionOfServiceTemplateImpl provisionOfServiceTemplate;

    @MockBean
    protected RepositoryClientSrv.Iface dominantClient;

    @MockBean
    protected PartyManagementSrv.Iface partyManagementClient;

    private static TestContainers testContainers = TestContainersBuilder.builderWithTestContainers(getTestContainersParametersSupplier())
            .addPostgresqlTestContainer()
            .build();

    protected String partyId = "TestPartyID";
    protected String shopId = "TestShopID";
    protected String contractId = "TestContractIdID";
    protected Party testParty = getTestParty(partyId, shopId, contractId);

    @Before
    public void setUp() throws Exception {
        ShopAccountingModel shopAccountingModel = random(ShopAccountingModel.class);

        given(statisticService.getCapturedPaymentsIterator(anyString(), anyString(), any(), any())).willReturn(getStatPayment());
        given(statisticService.getRefundsIterator(anyString(), anyString(), any(), any())).willReturn(getStatRefund());
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(Instant.class)))
                .willReturn(shopAccountingModel);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(), any(Instant.class)))
                .willReturn(shopAccountingModel);

        when(storageService.saveFile(any())).thenAnswer(i -> random(FileMeta.class));
        when(storageService.getFileUrl(anyString(), anyString(), any())).thenReturn(new URL("http://IP:4567/foldername/1234?abc=xyz"));

        when(paymentRegistryTemplate.accept(any())).thenCallRealMethod();
        doNothing().when(paymentRegistryTemplate).processReportTemplate(any(), any());

        when(provisionOfServiceTemplate.accept(any())).thenCallRealMethod();
        doNothing().when(provisionOfServiceTemplate).processReportTemplate(any(), any());

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(testParty);
        given(partyManagementClient.getMetaData(any(), any(), any()))
                .willReturn(Value.b(true));

        given(dominantClient.checkoutObject(any(), eq(Reference.payment_institution(new PaymentInstitutionRef(1)))))
                .willReturn(buildPaymentInstitutionObject(new PaymentInstitutionRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.calendar(new CalendarRef(1)))))
                .willReturn(buildPaymentCalendarObject(new CalendarRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.business_schedule(new BusinessScheduleRef(1)))))
                .willReturn(buildPayoutScheduleObject(new BusinessScheduleRef(1)));
    }

    @ClassRule
    public static final FailureDetectingExternalResource resource = new FailureDetectingExternalResource() {

        @Override
        protected void starting(Description description) {
            testContainers.startTestContainers();
        }

        @Override
        protected void failed(Throwable e, Description description) {
            log.warn("Test Container running was failed ", e);
        }

        @Override
        protected void finished(Description description) {
            testContainers.stopTestContainers();
        }
    };

    public static class Initializer extends ConfigFileApplicationContextInitializer {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            super.initialize(configurableApplicationContext);
            TestPropertyValues.of(
                    testContainers.getEnvironmentProperties(
                            environmentProperties -> {
                            }
                    )
            )
                    .applyTo(configurableApplicationContext);
        }
    }

    private static Supplier<TestContainersParameters> getTestContainersParametersSupplier() {
        return () -> {
            TestContainersParameters testContainersParameters = new TestContainersParameters();
            testContainersParameters.setPostgresqlJdbcUrl("jdbc:postgresql://localhost:5432/reporter");

            return testContainersParameters;
        };
    }

    private Iterator<StatPayment> getStatPayment() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public StatPayment next() {
                return new StatPayment();
            }
        };
    }

    private Iterator<StatRefund> getStatRefund() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public StatRefund next() {
                return new StatRefund();
            }
        };
    }
}
