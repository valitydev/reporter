package com.rbkmoney.reporter.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${storage.endpoint}")
    String endpoint;

    @Value("${storage.signingRegion}")
    String signingRegion;

    @Value("${storage.accessKey:}")
    String accessKey;

    @Value("${storage.secretKey:}")
    String secretKey;

    @Bean
    public AmazonS3 storageClient(AWSCredentialsProviderChain credentialsProviderChain, ClientConfiguration clientConfiguration) {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProviderChain)
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint, signingRegion)
                )
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    @Bean
    public AWSCredentialsProviderChain credentialsProviderChain() {
        return new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(
                                accessKey,
                                secretKey
                        )
                )
        );
    }

    @Bean
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setProtocol(Protocol.HTTP);
        clientConfiguration.setSignerOverride("S3SignerType");
        return clientConfiguration;
    }

    @Bean
    public TransferManager transferManager(AmazonS3 s3Client) {
        return TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .build();
    }

}
