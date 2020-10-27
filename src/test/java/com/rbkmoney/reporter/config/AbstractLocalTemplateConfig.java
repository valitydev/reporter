package com.rbkmoney.reporter.config;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.rbkmoney.easyway.AbstractTestUtils;
import com.rbkmoney.reporter.dao.impl.AdjustmentDaoImpl;
import com.rbkmoney.reporter.dao.impl.InvoiceDaoImpl;
import com.rbkmoney.reporter.dao.impl.PaymentDaoImpl;
import com.rbkmoney.reporter.dao.impl.RefundDaoImpl;
import com.rbkmoney.reporter.service.impl.LocalReportCreatorServiceImpl;
import com.rbkmoney.reporter.service.impl.LocalStatisticServiceImpl;
import com.rbkmoney.reporter.service.impl.PartyServiceImpl;
import com.rbkmoney.reporter.service.impl.ReportCreatorServiceImpl;
import com.rbkmoney.reporter.template.LocalPaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.template.PaymentRegistryTemplateImpl;
import com.rbkmoney.reporter.template.ProvisionOfServiceTemplateImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

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
                ApplicationConfig.class,
                LocalStatisticServiceImpl.class,
                LocalReportCreatorServiceImpl.class,
                LocalPaymentRegistryTemplateImpl.class,
        },
        initializers = AbstractLocalTemplateConfig.Initializer.class
)
@TestPropertySource("classpath:application.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractLocalTemplateConfig extends AbstractTestUtils {

    private static final int PORT = 15432;
    private static final String dbName = "reporter";
    private static final String dbUser = "postgres";
    private static final String dbPassword = "postgres";
    private static final String jdbcUrl = "jdbc:postgresql://localhost:" + PORT + "/" + dbName;

    private static EmbeddedPostgres postgres;

    @After
    public void destroy() throws IOException {
        if (postgres != null) {
            postgres.close();
            postgres = null;
        }
    }

    private static void startPgServer() {
        try {
            log.info("The PG server is starting...");
            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
            String dbDir = prepareDbDir();
            log.info("Dir for PG files: " + dbDir);
            builder.setDataDirectory(dbDir);
            builder.setPort(PORT);
            postgres = builder.start();
            log.info("The PG server was started!");
        } catch (IOException e) {
            log.error("An error occurred while starting server ", e);
            e.printStackTrace();
        }
    }

    private static void createDatabase() {
        try (Connection conn = postgres.getPostgresDatabase().getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE DATABASE " + dbName);
            statement.close();
        } catch (SQLException e) {
            log.error("An error occurred while creating the database "+ dbName, e);
            e.printStackTrace();
        }
    }

    private static String prepareDbDir() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDate = dateFormat.format(new Date());
        String dir = "target" + File.separator + "pgdata_" + currentDate;
        log.info("Postgres source files in {}", dir);
        return dir;
    }

    private DataSource getDataSource() {
        return postgres.getDatabase(dbUser, dbName);
    }

    public static class Initializer extends ConfigFileApplicationContextInitializer {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            super.initialize(configurableApplicationContext);
            TestPropertyValues.of("spring.datasource.url=" + jdbcUrl,
                    "spring.datasource.username=" + dbUser,
                    "spring.datasource.password=" + dbPassword,
                    "flyway.url=" + jdbcUrl,
                    "flyway.user=" + dbUser,
                    "flyway.password=" + dbPassword)
                    .applyTo(configurableApplicationContext);

            if (postgres == null) {
                startPgServer();
                createDatabase();
            }
        }
    }
}
