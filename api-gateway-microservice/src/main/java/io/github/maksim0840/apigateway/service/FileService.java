package io.github.maksim0840.apigateway.service;

import io.github.maksim0840.apigateway.config.properties.S3Properties;
import io.github.maksim0840.apigateway.dto.FileInfo;
import io.github.maksim0840.apigateway.enums.FileType;
import io.github.maksim0840.apigateway.util.RandomIDGenerator;
import io.github.maksim0840.internalapi.common.v1.s3.S3StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class FileService {
    private static final String HTML_DIR_PREFIX = "html";
        private static final String IMAGES_DIR_PREFIX = "html";

    private final S3StorageService s3StorageService;

    public FileService(S3Client s3Client, S3Properties s3Properties) {
        this.s3StorageService = new S3StorageService(s3Client, s3Properties.bucketName());
    }

    public String getHtmlOutDir(String sessionId) {
        return sessionId + "/" + HTML_DIR_PREFIX;
    }

    public String getImagesOutDir(String sessionId) {
        return sessionId + "/" + IMAGES_DIR_PREFIX;
    }

    public String upload(String sessionId, FileType fileType, MultipartFile file) throws IOException {
        String fileId = RandomIDGenerator.generateString(16);
        String objectKey = switch (fileType) {
            case HTML -> sessionId + "/html/" + fileId;
            case IMG -> sessionId + "/imgs/" + fileId;
        };
        s3StorageService.uploadFileBytes(objectKey, file.getBytes());
        return objectKey;
    }

    public FileInfo download(String objectKey) {
        String fileName = Path.of(objectKey).getFileName().toString();
        byte[] fileBytes = s3StorageService.downloadFileBytes(objectKey);
        return new FileInfo(fileName, fileBytes);
    }

    public List<String> getObjectKeysByPrefix(String prefix) {
        return s3StorageService.getObjectKeysByPrefix(prefix);
    }
}
