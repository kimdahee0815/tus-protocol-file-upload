package com.furence.tus.demo.file.service;

import com.furence.tus.demo.file.domain.dto.RecFileSaveRequest;
import com.furence.tus.demo.file.domain.dto.UploadFileSaveRequest;
import com.furence.tus.demo.file.repository.RecFileRepository;
import com.furence.tus.demo.file.repository.UploadFileRepository;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final TusFileUploadService tusFileUploadService;
    private final RecFileRepository recFileRepository;
    private final UploadFileRepository uploadFileRepository;

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

    /**
     * ✅ 서버 내부 고유 파일명 생성
     * 예: 20251105T103012_UUID_원본이름
     */
    private String createUniqueFileName(String originalFileName) {
        String ext = "";
        int dotIdx = originalFileName.lastIndexOf(".");
        if (dotIdx != -1) {
            ext = originalFileName.substring(dotIdx);
        }

        String uniquePrefix = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        return uniquePrefix + ext;
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
        File dir = new File("C:/uploads/");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, filename);
        FileUtils.copyInputStreamToFile(is, file);
        log.info("File created at: {}", file.getAbsolutePath());
        return file;
    }
}
