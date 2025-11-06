package com.furence.tus.demo.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidator {

    private final FileUploadProperties fileUploadProperties;

    /**
     * 음성 파일 확장자 검증
     */
    public void validateVoiceFileExtension(String fileName) {
        String extension = extractExtension(fileName);

        if (!fileUploadProperties.getVoiceAllowedExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("허용되지 않은 음성 파일 확장자입니다. 파일명: %s, 허용 확장자: %s",
                            fileName,
                            fileUploadProperties.getVoiceAllowedExtensions())
            );
        }
    }

    /**
     * 음성 파일 Content-Type 검증
     */
    public void validateVoiceContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            log.warn("Content-Type이 비어있습니다.");
            return;
        }

        if (!fileUploadProperties.getVoiceAllowedContentTypes().contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("허용되지 않은 음성 파일 타입입니다. Content-Type: %s, 허용 타입: %s",
                            contentType,
                            fileUploadProperties.getVoiceAllowedContentTypes())
            );
        }
    }

    /**
     * SMS 파일 확장자 검증 (이미지 등)
     */
    public void validateSmsFileExtension(String fileName) {
        String extension = extractExtension(fileName);

        if (!fileUploadProperties.getSmsAllowedExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("허용되지 않은 SMS 파일 확장자입니다. 파일명: %s, 허용 확장자: %s",
                            fileName,
                            fileUploadProperties.getSmsAllowedExtensions())
            );
        }
    }

    /**
     * 제한된 확장자 검증
     */
    public void validateRestrictedExtension(String fileName) {
        String extension = extractExtension(fileName);

        if (fileUploadProperties.getRestrictedFileExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("업로드가 금지된 파일 확장자입니다. 파일명: %s, 금지 확장자: %s",
                            fileName,
                            fileUploadProperties.getRestrictedFileExtensions())
            );
        }
    }

    /**
     * 제한된 Content-Type 검증
     */
    public void validateRestrictedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }

        if (fileUploadProperties.getRestrictedFileContentType().contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("업로드가 금지된 파일 타입입니다. Content-Type: %s",
                            contentType)
            );
        }
    }

    /**
     * 파일 크기 검증
     */
    public void validateFileSize(long fileSize) {
        if (fileSize > fileUploadProperties.getMaxSize()) {
            throw new IllegalArgumentException(
                    String.format("파일 크기가 제한을 초과했습니다. 현재: %d bytes, 최대: %d bytes",
                            fileSize,
                            fileUploadProperties.getMaxSize())
            );
        }
    }

    /**
     * 파일명에서 확장자 추출
     */
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일명이 비어있습니다.");
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("파일 확장자를 찾을 수 없습니다: " + fileName);
        }

        return fileName.substring(lastDotIndex + 1);
    }
}