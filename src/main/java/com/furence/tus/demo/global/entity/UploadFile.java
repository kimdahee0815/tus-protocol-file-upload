package com.furence.tus.demo.global.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(nullable = false, length = 256)
    private String path;

    @Column(length = 256)
    private String url;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "client_file_name", nullable = false, length = 256)
    private String clientFileName;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "create_timestamp", nullable = false)
    private LocalDateTime createTimestamp = LocalDateTime.now();

    @PrePersist
    public void onCreate() {
        this.createTimestamp = LocalDateTime.now();
    }
}