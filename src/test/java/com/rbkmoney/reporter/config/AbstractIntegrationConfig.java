package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.damsel.domain.CalendarRef;
import com.rbkmoney.damsel.domain.PaymentInstitutionRef;
import com.rbkmoney.damsel.domain.Reference;
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
import com.rbkmoney.reporter.ReporterApplication;
import com.rbkmoney.reporter.model.ShopAccountingModel;
import com.rbkmoney.reporter.service.StatisticService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.FailureDetectingExternalResource;

import java.time.Instant;
import java.util.Iterator;
import java.util.function.Supplier;

import static com.rbkmoney.reporter.utils.TestDataUtil.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = ReporterApplication.class, initializers = AbstractIntegrationConfig.Initializer.class)
@TestPropertySource("classpath:application.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractIntegrationConfig extends AbstractTestUtils {

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private RepositoryClientSrv.Iface dominantClient;

    @MockBean
    private PartyManagementSrv.Iface partyManagementClient;

    private static TestContainers testContainers = TestContainersBuilder.builderWithTestContainers(getTestContainersParametersSupplier())
            .addPostgresqlTestContainer()
            .addCephTestContainer()
            .build();

    protected String partyId = "TestPartyID";
    protected String shopId = "TestShopID";
    protected String contractId = random(String.class);
    protected Instant fromTime = random(Instant.class);
    protected Instant toTime = random(Instant.class);

    @Before
    public void setUp() throws Exception {
        given(statisticService.getCapturedPaymentsIterator(anyString(), anyString(), any(), any())).willReturn(getStatPayment());

        given(statisticService.getRefundsIterator(anyString(), anyString(), any(), any())).willReturn(getStatRefund());

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(getTestParty(partyId, shopId, contractId));
        given(partyManagementClient.getMetaData(any(), any(), any()))
                .willReturn(Value.b(true));
        ShopAccountingModel shopAccountingModel = random(ShopAccountingModel.class);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(Instant.class)))
                .willReturn(shopAccountingModel);
        given(statisticService.getShopAccounting(anyString(), anyString(), anyString(), any(), any(Instant.class)))
                .willReturn(shopAccountingModel);
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
