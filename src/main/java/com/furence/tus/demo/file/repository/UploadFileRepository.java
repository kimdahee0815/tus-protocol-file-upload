package com.furence.tus.demo.file.repository;

import com.furence.tus.demo.global.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {
}