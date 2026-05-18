package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.FileWithContent;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.FileInfoDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType;
import io.github.maksim0840.apigateway.service.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public FileInfoDTO s3UploadFile(
            @RequestParam String sessionId,
            @RequestParam FileType fileType,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        System.out.println("s3UploadFile");
        FileInfoDTO fileInfo = fileService.upload(sessionId, fileType, file);
        return fileInfo;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> s3DownloadFile(@RequestParam String filePath) {
        System.out.println("s3DownloadFile: " + filePath);
        FileWithContent info = fileService.download(filePath);
        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(info.fileBytes().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + info.fileName() + "\""
                )
                .body(info.fileBytes());
    }

    @DeleteMapping("/delete")
    public void s3DeleteFile(@RequestParam String filePath) {
        fileService.delete(filePath);
    }
}
