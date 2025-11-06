package com.furence.tus.demo.file.service;

import com.furence.tus.demo.file.domain.dto.RecFileSaveRequest;
import com.furence.tus.demo.file.domain.dto.UploadFileSaveRequest;
import com.furence.tus.demo.file.repository.RecFileRepository;
import com.furence.tus.demo.file.repository.UploadFileRepository;
import com.furence.tus.demo.global.config.FileValidator;
import com.furence.tus.demo.global.config.FileUploadProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final TusFileUploadService tusFileUploadService;
    private final RecFileRepository recFileRepository;
    private final UploadFileRepository uploadFileRepository;
    private final FileValidator fileValidator;
    private final FileUploadProperties fileUploadProperties;

    @Transactional
    public void saveRecFile(RecFileSaveRequest dto) {
        recFileRepository.save(dto.toEntity());
    }

    @Transactional
    public void saveUploadFile(UploadFileSaveRequest dto) {
        uploadFileRepository.save(dto.toEntity());
    }

    public void processUpload(HttpServletRequest request, HttpServletResponse response) {
        try {
            // POST 요청 시 (업로드 시작 단계)에서 파일 검증
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                validateUploadRequest(request);
            }

            tusFileUploadService.process(request, response);

            UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(request.getRequestURI());
            if (uploadInfo == null) return;

            // 🟢 업로드가 아직 진행 중이라면 (progress 중)
            if (uploadInfo.isUploadInProgress()) {
                log.debug("Upload in progress... offset={}", uploadInfo.getOffset());
                return; // 아직 완료되지 않았으므로 저장 처리하지 않음
            }

            // ✅ 업로드 완료 시점
            String clientFileName = request.getHeader("X-Client-File-Name");
            if (clientFileName == null || clientFileName.isBlank()) {
                clientFileName = uploadInfo.getFileName(); // fallback
            }

            // ✅ 1️⃣ serverFileName: 기존 메타데이터에서 불러오거나 새로 생성
            String serverFileName = uploadInfo.getMetadata().get("serverFileName");
            if (serverFileName == null || serverFileName.isBlank()) {
                serverFileName = request.getHeader("X-Server-File-Name");
            }

            // ✅ 2️⃣ 파일 생성
            File file = createFile(tusFileUploadService.getUploadedBytes(request.getRequestURI()), serverFileName);

            // ✅ 3️⃣ 업로드 정보 삭제 (cleanup)
            tusFileUploadService.deleteUpload(request.getRequestURI());

            // ✅ 4️⃣ DB 저장
            saveFileInfoToDatabase(request, file, serverFileName, clientFileName);

            log.info("Upload completed: clientFile={}, serverFile={}", clientFileName, serverFileName);

        } catch (IOException | TusException e) {
            log.error("TUS upload exception occurred. message={}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 업로드 요청 검증 (POST 시점에 호출)
     */
    private void validateUploadRequest(HttpServletRequest request) {
        String clientFileName = request.getHeader("X-Client-File-Name");
        String uploadLength = request.getHeader("Upload-Length");
        String uploadMetadata = request.getHeader("Upload-Metadata");

        // 파일명 검증
        if (clientFileName == null || clientFileName.isBlank()) {
            log.warn("X-Client-File-Name header is missing");
        } else {
            // 제한된 확장자 검증 (exe, bat 등)
            fileValidator.validateRestrictedExtension(clientFileName);

            // 음성 파일 확장자 검증 (wav, mp3, m4a 등)
            fileValidator.validateVoiceFileExtension(clientFileName);
        }

        // 파일 크기 검증
        if (uploadLength != null && !uploadLength.isBlank()) {
            try {
                long fileSize = Long.parseLong(uploadLength);
                fileValidator.validateFileSize(fileSize);
            } catch (NumberFormatException e) {
                log.warn("Invalid Upload-Length header: {}", uploadLength);
            }
        }

        // Content-Type 검증 (메타데이터에서 추출)
        if (uploadMetadata != null && !uploadMetadata.isBlank()) {
            String contentType = extractContentTypeFromMetadata(uploadMetadata);
            if (contentType != null) {
                fileValidator.validateRestrictedContentType(contentType);
                fileValidator.validateVoiceContentType(contentType);
            }
        }

        log.info("File validation passed for: {}", clientFileName);
    }

    /**
     * Upload-Metadata에서 Content-Type 추출
     * 형식: "filename dGVzdC5tcDQ=,filetype YXVkaW8vbXA0"
     */
    private String extractContentTypeFromMetadata(String metadata) {
        try {
            String[] pairs = metadata.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.trim().split(" ", 2);
                if (keyValue.length == 2 && "filetype".equalsIgnoreCase(keyValue[0])) {
                    byte[] decoded = java.util.Base64.getDecoder().decode(keyValue[1]);
                    return new String(decoded);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract content type from metadata: {}", metadata);
        }
        return null;
    }

    private void saveFileInfoToDatabase(HttpServletRequest request, File file, String serverFileName, String clientFileName) {
        try {
            // upload_file 저장
            UploadFileSaveRequest uploadFile = UploadFileSaveRequest.builder()
                    .path(file.getAbsolutePath())
                    .url("/uploads/" + serverFileName)
                    .fileName(serverFileName)
                    .clientFileName(clientFileName)
                    .fileSize((int) file.length())
                    .build();

            saveUploadFile(uploadFile);

            // rec_file 저장
            RecFileSaveRequest recFile = RecFileSaveRequest.builder()
                    .tenantId(parseLongHeader(request, "X-Tenant-Id"))
                    .recDate(request.getHeader("X-Rec-Date"))
                    .recHtime(request.getHeader("X-Rec-Htime"))
                    .recTime(request.getHeader("X-Rec-Time"))
                    .extNo(request.getHeader("X-Ext-No"))
                    .fileName(serverFileName)
                    .build();

            saveRecFile(recFile);

        } catch (Exception e) {
            log.error("Failed to save file info to database. error={}", e.getMessage(), e);
        }
    }

    private Long parseLongHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.trim().isEmpty()) {
            log.warn("Header {} is null or empty", headerName);
            return null;
        }
        try {
            return Long.parseLong(headerValue);
        } catch (NumberFormatException e) {
            log.error("Failed to parse header {} value: {}", headerName, headerValue);
            return null;
        }
    }

    private File createFile(InputStream is, String filename) throws IOException {
        // YAML 설정에서 경로 읽어오기
        File dir = new File(fileUploadProperties.getPath());
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, filename);
        FileUtils.copyInputStreamToFile(is, file);
        log.info("File created at: {}", file.getAbsolutePath());
        return file;
    }
}