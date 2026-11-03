package dev.vality.reporter.config;

import com.robothy.s3.rest.LocalS3;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class EmbeddedS3Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        LocalS3 localS3 = LocalS3.builder().port(-1).build();
        localS3.start();
        var registry = (DefaultSingletonBeanRegistry) context.getBeanFactory();
        registry.registerSingleton("embeddedS3", localS3);
        registry.registerDisposableBean("embeddedS3", localS3::shutdown);
        TestPropertyValues.of(
                "storage.endpoint=http://localhost:" + localS3.getPort(),
                "storage.accessKey=test",
                "storage.secretKey=test",
                "storage.bucketName=test",
                "storage.signingRegion=us-east-1",
                "storage.client.protocol=HTTP"
        ).applyTo(context);
    }

}
