package com.rbkmoney.reporter.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@ConfigurationProperties(prefix = "payouter.polling")
@Data
public class PayoutProperties {

    private Resource url;
    private int housekeeperTimeout;
    private int maxPoolSize;
    private int maxQuerySize;
    private int delay;
    private int retryDelay;

}
