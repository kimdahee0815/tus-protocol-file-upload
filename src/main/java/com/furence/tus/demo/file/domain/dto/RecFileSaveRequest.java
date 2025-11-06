package com.furence.tus.demo.file.domain.dto;

import com.furence.tus.demo.global.entity.RecFile;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecFileSaveRequest {
    private Long tenantId;
    private String recDate;
    private String recHtime;
    private String recTime;
    private String extNo;
    private String fileName;

    public RecFile toEntity() {
        return RecFile.builder()
                .tenantId(tenantId)
                .recDate(recDate)
                .recHtime(recHtime)
                .recTime(recTime)
                .extNo(extNo)
                .fileName(fileName)
                .build();
    }
}

