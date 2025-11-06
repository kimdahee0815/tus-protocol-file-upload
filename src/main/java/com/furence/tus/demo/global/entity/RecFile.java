package com.furence.tus.demo.global.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rs4_ser_recfile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecFile {

    @Id
    @Column(name = "r_sequence")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequence;

    @Column(name = "r_tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "r_rec_date", nullable = false, length = 8)
    private String recDate; // yyyyMMdd

    @Column(name = "r_rec_htime", nullable = false, length = 2)
    private String recHtime; // hh

    @Column(name = "r_rec_time", nullable = false, length = 8)
    private String recTime; // hh:mm:ss

    @Column(name = "r_ext_no", nullable = false)
    private String extNo;

    @Column(name = "r_file_name", nullable = false)
    private String fileName;
}