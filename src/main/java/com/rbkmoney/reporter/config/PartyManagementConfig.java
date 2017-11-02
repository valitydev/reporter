package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Created by tolkonepiu on 17/07/2017.
 */
@Configuration
public class PartyManagementConfig {

    @Value("${partyManagement.url}")
    Resource resource;

    @Bean
    public PartyManagementSrv.Iface partyManagementClient() throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI()).build(PartyManagementSrv.Iface.class);
    }
}
