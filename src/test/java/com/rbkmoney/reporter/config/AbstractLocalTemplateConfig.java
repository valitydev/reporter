package com.rbkmoney.reporter.config;

import com.rbkmoney.reporter.dao.impl.*;
import com.rbkmoney.reporter.handler.comparing.PaymentRegistryReportComparingHandler;
import com.rbkmoney.reporter.handler.comparing.ProvisionOfServiceReportComparingHandler;
import com.rbkmoney.reporter.service.impl.LocalReportCreatorServiceImpl;
import com.rbkmoney.reporter.service.impl.LocalStatisticServiceImpl;
import com.rbkmoney.reporter.service.impl.PartyServiceImpl;
import com.rbkmoney.reporter.service.impl.ReportCreatorServiceImpl;
import com.rbkmoney.reporter.template.LocalPaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.template.LocalProvisionOfServiceTemplateImpl;
import com.rbkmoney.reporter.template.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.template.ProvisionOfServiceTemplateImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {
                ReportCreatorServiceImpl.class,
                PartyServiceImpl.class,
                PaymentRegistryTemplateImpl.class,
                ProvisionOfServiceTemplateImpl.class,
                DataSourceAutoConfiguration.class,
                JdbcTemplateAutoConfiguration.class,
                TransactionAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                InvoiceDaoImpl.class,
                PaymentDaoImpl.class,
                RefundDaoImpl.class,
                AdjustmentDaoImpl.class,
                AggregatesDaoImpl.class,
                PayoutDaoImpl.class,
                ChargebackDaoImpl.class,
                ReportComparingDataDaoImpl.class,
                ApplicationConfig.class,
                LocalStatisticServiceImpl.class,
                LocalReportCreatorServiceImpl.class,
                LocalPaymentRegistryTemplateImpl.class,
                LocalProvisionOfServiceTemplateImpl.class,
                PaymentRegistryReportComparingHandler.class,
                ProvisionOfServiceReportComparingHandler.class
        },
        initializers = AbstractLocalTemplateConfig.Initializer.class
)
@Slf4j
public abstract class AbstractLocalTemplateConfig extends AbstractDaoConfig {
}
