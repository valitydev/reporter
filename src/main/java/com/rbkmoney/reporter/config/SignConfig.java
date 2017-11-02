package com.rbkmoney.reporter.config;

import com.rbkmoney.damsel.signer.SignerSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class SignConfig {

    @Value("${signer.url}")
    private Resource resource;

    @Bean
    public SignerSrv.Iface signerClient() throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI())
                .build(SignerSrv.Iface.class);
    }

}
