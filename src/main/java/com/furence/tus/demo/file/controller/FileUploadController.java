package com.furence.tus.demo.file.controller;

import com.furence.tus.demo.file.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * 참고: https://qiita.com/Amtkxa/items/6a605209bcc6eb477ec0
 * 참고: https://github.com/tus/tus-js-client/blob/master/docs/usage.md
 * 참고: https://stackoverflow.com/questions/72917279/how-to-upload-multiple-files-using-tus-protocol
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @RequestMapping(
            value = {"/api/tus/file/upload", "/api/tus/file/upload/**"},
            method = {RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS}
    )
    public ResponseEntity<Object> uploadWithTus(HttpServletRequest request, HttpServletResponse response) {
        // 서비스에서 업로드 처리 및 DB 저장
        fileUploadService.processUpload(request, response);

        // HTTP 응답 상태 코드 결정
        HttpStatus status = determineHttpStatus(request.getMethod());

        return buildResponse(status);
    }

    /**
     * HTTP 메서드에 따른 상태 코드 결정
     */
    private HttpStatus determineHttpStatus(String method) {
        return switch (method) {
            case "POST" -> HttpStatus.CREATED;      // 201
            case "PATCH", "OPTIONS", "HEAD" -> HttpStatus.OK;  // 200
            default -> HttpStatus.INTERNAL_SERVER_ERROR;  // 500
        };
    }

    /**
     * HTTP 응답 생성
     */
    private ResponseEntity<Object> buildResponse(HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");

        return ResponseEntity.status(status)
                .headers(headers)
                .build();
    }
}