package com.rbkmoney.reporter;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.time.Duration;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = ReporterApplication.class, initializers = AbstractIntegrationTest.Initializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractIntegrationTest {

    public static String AWS_ACCESS_KEY = "test";
    public static String AWS_SECRET_KEY = "test";
    public static String BUCKET_NAME = "TEST";

    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:9.6")
            .withStartupTimeout(Duration.ofMinutes(5));

    @ClassRule
    public static GenericContainer cephContainer = new GenericContainer("ceph/demo:tag-stable-3.0-jewel-ubuntu-16.04")
            .withEnv("RGW_NAME", "localhost")
            .withEnv("NETWORK_AUTO_DETECT", "4")
            .withEnv("CEPH_DEMO_UID", "ceph-test")
            .withEnv("CEPH_DEMO_ACCESS_KEY", AWS_ACCESS_KEY)
            .withEnv("CEPH_DEMO_SECRET_KEY", AWS_SECRET_KEY)
            .withEnv("CEPH_DEMO_BUCKET", BUCKET_NAME)
            .withExposedPorts(5000, 80)
            .waitingFor(
                    new HttpWaitStrategy()
                            .forPath("/api/v0.1/health")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(10))
            );

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            EnvironmentTestUtils.addEnvironment("testcontainers", configurableApplicationContext.getEnvironment(),
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "flyway.url=" + postgres.getJdbcUrl(),
                    "flyway.user=" + postgres.getUsername(),
                    "flyway.password=" + postgres.getPassword(),
                    "storage.endpoint=" + cephContainer.getContainerIpAddress() + ":" + cephContainer.getMappedPort(80),
                    "storage.signingRegion=RU",
                    "storage.bucketName=" + BUCKET_NAME,
                    "storage.accessKey=" + AWS_ACCESS_KEY,
                    "storage.secretKey=" + AWS_SECRET_KEY
            );
        }
    }

    @LocalServerPort
    protected int port;



}
