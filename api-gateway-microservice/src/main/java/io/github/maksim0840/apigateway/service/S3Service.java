package io.github.maksim0840.apigateway.service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class S3Service {
    private final S3Client client;
    private final String bucketName;

    public S3Service(S3Client client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;
    }

    public boolean fileExists(String s3ObjectKey) {
        try {
            client.headObject(o -> o.bucket(bucketName).key(s3ObjectKey));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public void uploadFileBytes(String s3ObjectKey, byte[] fileBytes) {
        client.putObject(
                o -> o.bucket(bucketName).key(s3ObjectKey),
                RequestBody.fromBytes(fileBytes)
        );
    }

    public byte[] downloadFileBytes(String s3ObjectKey) {
        ResponseBytes<GetObjectResponse> response = client.getObjectAsBytes(o -> o.bucket(bucketName).key(s3ObjectKey));
        return response.asByteArray();
    }

    public void deleteFile(String s3ObjectKey) {
        client.deleteObject(o -> o.bucket(bucketName).key(s3ObjectKey));
    }
}
