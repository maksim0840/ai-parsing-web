package io.github.maksim0840.parsingtaskorchestrator.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Properties(
    String accessKey,
    String secretKey,
    String endpointUrl,
    boolean pathStyleAccess,
    String regionName,
    String bucketName
) {
}
