package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.domain_config.RepositorySrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class DomainConfig {

    @Value("${domainConfig.url}")
    Resource resource;

    @Bean
    public RepositoryClientSrv.Iface dominantClient() throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI()).build(RepositoryClientSrv.Iface.class);
    }

}
