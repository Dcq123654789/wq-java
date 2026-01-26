package com.example.wq.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.example.wq.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileController {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.accessKeyId}")
    private String accessKeyId;

    @Value("${oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${oss.bucketName}")
    private String bucketName;

    @Value("${oss.dir:uploads/}")
    private String dir;

    /** 允许的文件类型 */
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "csv",
            "zip", "rar", "7z"
    );

    /** 最大文件大小 10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostMapping("/upload")
    @Operation(summary = "上传文件到OSS", description = "支持图片、文档、文本和压缩包，最大10MB")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            validateFile(file);

            // 上传到OSS
            String fileUrl = uploadToOSS(file);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("size", file.getSize());

            log.info("文件上传成功: {}", fileUrl);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            log.warn("文件上传失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（最大10MB）");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名格式不正确");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_FILE_TYPES.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }

    /**
     * 上传文件到阿里云OSS
     */
    private String uploadToOSS(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成唯一文件名：日期/UUID.扩展名
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String objectName = dir + dateStr + "/" + fileName;

        OSS ossClient = null;
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 上传文件
            ossClient.putObject(bucketName, objectName, inputStream, metadata);

            // 生成文件URL
            String fileUrl = "https://" + bucketName + "." + endpoint + "/" + objectName;

            return fileUrl;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输入流失败", e);
                }
            }
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
