package com.furence.tus.demo.file.repository;

import com.furence.tus.demo.global.entity.RecFile;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecFileRepositoryImpl implements RecFileRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    @Override
    public void insertRecFile(RecFile recFile) {
        em.persist(recFile);
    }
}