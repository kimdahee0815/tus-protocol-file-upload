package com.furence.tus.demo.file.domain.dto;

import com.furence.tus.demo.global.entity.UploadFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileSaveRequest {
    private String path;
    private String url;
    private String fileName;
    private String clientFileName;
    private Integer fileSize;

    public UploadFile toEntity() {
        return UploadFile.builder()
                .path(path)
                .url(url)
                .fileName(fileName)
                .clientFileName(clientFileName)
                .fileSize(fileSize)
                .build();
    }
}