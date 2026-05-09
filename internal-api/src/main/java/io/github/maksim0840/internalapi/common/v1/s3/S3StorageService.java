package io.github.maksim0840.internalapi.common.v1.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

public class S3StorageService {
    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public boolean fileExists(String objectKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    public void uploadFileBytes(String objectKey, byte[] fileBytes) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(fileBytes));
    }

    public byte[] downloadFileBytes(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    public void deleteFile(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
    }

    public List<String> getObjectKeysByPrefix(String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder();
            requestBuilder.bucket(bucketName);
            requestBuilder.prefix(prefix);
            if (continuationToken != null) requestBuilder.continuationToken(continuationToken);

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            for (S3Object object : response.contents()) {
                keys.add(object.key());
            }
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);

        return keys;
    }
}
