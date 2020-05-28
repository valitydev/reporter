package com.rbkmoney.reporter.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.merch_stat.MerchantStatisticsSrv;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.damsel.signer.SignerSrv;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import com.rbkmoney.sink.common.parser.impl.PartyEventDataMachineEventParser;
import com.rbkmoney.sink.common.serialization.BinaryDeserializer;
import com.rbkmoney.sink.common.serialization.impl.PartyEventDataDeserializer;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class ApplicationConfig {

    @Bean
    public RepositoryClientSrv.Iface dominantClient(
            @Value("${domainConfig.url}") Resource resource,
            @Value("${domainConfig.timeout}") int timeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(timeout)
                .build(RepositoryClientSrv.Iface.class);
    }

    @Bean
    public PartyManagementSrv.Iface partyManagementClient(
            @Value("${partyManagement.url}") Resource resource,
            @Value("${partyManagement.timeout}") int timeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(timeout)
                .build(PartyManagementSrv.Iface.class);
    }

    @Bean
    public SignerSrv.Iface signerClient(
            @Value("${signer.url}") Resource resource,
            @Value("${signer.timeout}") int timeout
            ) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(timeout)
                .withAddress(resource.getURI()).build(SignerSrv.Iface.class);
    }

    @Bean
    public MerchantStatisticsSrv.Iface merchantStatisticsClient(
            @Value("${magista.url}") Resource resource,
            @Value("${magista.timeout}") int timeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .withNetworkTimeout(timeout)
                .build(MerchantStatisticsSrv.Iface.class);
    }

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
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
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
