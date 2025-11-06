package com.furence.tus.demo.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileUploadProperties {

    private String path;
    private Long maxSize;
    private List<String> smsAllowedExtensions;
    private List<String> restrictedFileExtensions;
    private List<String> restrictedFileContentType;
    private List<String> voiceAllowedExtensions;
    private List<String> voiceAllowedContentTypes;
}