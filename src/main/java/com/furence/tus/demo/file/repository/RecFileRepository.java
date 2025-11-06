package com.furence.tus.demo.file.repository;

import com.furence.tus.demo.global.entity.RecFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecFileRepository extends JpaRepository<RecFile, Long>, RecFileRepositoryCustom {
}
