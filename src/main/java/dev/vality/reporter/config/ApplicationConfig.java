package dev.vality.reporter.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.vality.damsel.domain_config_v2.RepositoryClientSrv;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class ApplicationConfig {

    @Bean
    public InvoicingSrv.Iface invoicingThriftClient(
            @Value("${hellgate.invoicing.url}") Resource resource,
            @Value("${hellgate.invoicing.timeout}") int timeout
    )
            throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(timeout)
                .build(InvoicingSrv.Iface.class);
    }

    @Bean
    public RepositoryClientSrv.Iface dominantClient(
            @Value("${dominant.url}") Resource resource,
            @Value("${dominant.timeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI())
                .build(RepositoryClientSrv.Iface.class);
    }

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .changeDefaultPropertyInclusion(value -> value.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public ExecutorService reportsThreadPool(@Value("${report.batchSize}") int threadPoolSize) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("report-exec-%d")
                .setDaemon(true)
                .build();
        return Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    }

}
