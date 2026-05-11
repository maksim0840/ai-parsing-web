package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.config.properties.S3Properties;
import io.github.maksim0840.apigateway.dto.FileInfo;
import io.github.maksim0840.apigateway.enums.FileType;
import io.github.maksim0840.apigateway.service.FileService;
import io.github.maksim0840.internalapi.common.v1.s3.S3StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String s3UploadFile(
            @RequestParam String sessionId,
            @RequestParam FileType fileType,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        System.out.println("s3UploadFile");
        String filePath = fileService.upload(sessionId, fileType, file);
        return filePath;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> s3DownloadFile(@RequestParam String filePath) {
        System.out.println("s3DownloadFile: " + filePath);
        FileInfo info = fileService.download(filePath);
        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + info.fileName() + "\""
                )
                .body(info.fileBytes());
    }
}
