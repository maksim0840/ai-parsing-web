package io.github.maksim0840.apigateway.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;

public class S3Config {
    private static final String DEFAULT_BUCKET_NAME = "garage-default-bucket";
    private static final String TTL_7D_BUCKET_NAME = "garage-ttl-7d-bucket";

    public static S3Client getClient() {
        return S3Client.builder()
                .endpointOverride(URI.create("http://localhost:3900"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("GKb1b6b3a6cae1445a5a17a087", "549eff9a670f17cd878edb8f5ffa170f0f1935f96dcc2c501928730f47850f2c")
                ))
                .region(Region.of("garage"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build()
                )
                .build();
    }

    public static String getBucketName(boolean withTimeToLive) {
        return withTimeToLive ? TTL_7D_BUCKET_NAME : DEFAULT_BUCKET_NAME;
    }
}