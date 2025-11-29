package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.vo.UploadVO;
import com.lyz.util.FileParserUtil;
import com.lyz.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final MinioUtil minioUtil;
    private final FileParserUtil fileParserUtil;

    @Value("${upload.max-file-size:10485760}") // 默认10MB
    private long maxFileSize;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "image/gif", "image/webp"
    );

    private static final Set<String> REPORT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @PostMapping
    public Result<UploadVO> upload(@RequestParam("type") String type,
                                   @RequestParam("file") MultipartFile file) throws Exception {
        Long userId = UserContext.getUserId();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("type 不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(String.format("文件大小超出限制：最大 %d MB", maxFileSize / 1024 / 1024));
        }

        String contentType = file.getContentType();

        String prefix;
        if ("avatar".equalsIgnoreCase(type)) {
            if (!IMAGE_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("头像仅支持 JPG/PNG/GIF/WEBP 图片");
            }
            prefix = "avatars/" + userId;
        } else if ("report".equalsIgnoreCase(type)) {
            if (!REPORT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("报告仅支持 PDF 或 Word (DOC/DOCX)");
            }
            prefix = "reports/" + userId;
        } else {
            throw new IllegalArgumentException("type 必须是 avatar 或 report");
        }

        String ext = getExtension(file);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String objectName = timestamp + (ext.isEmpty() ? "" : ("." + ext));

        String objectKey = minioUtil.uploadFile(file, prefix, objectName);
        String url = minioUtil.getPublicUrl(objectKey);

        if ("report".equalsIgnoreCase(type)) {
            try {
                fileParserUtil.parseAndExtract(objectKey, userId);
                log.info("体检报告上传成功，开始异步解析: userId={}, objectKey={}", userId, objectKey);
            } catch (Exception e) {
                log.error("体检报告解析失败: userId={}, objectKey={}", userId, objectKey, e);
            }
        }

        UploadVO vo = new UploadVO();
        vo.setType(type.toLowerCase());
        vo.setUrl(url);

        String message = "上传成功";
        if ("report".equalsIgnoreCase(type)) {
            message = "上传成功，正在异步解析体检报告";
        }

        return Result.success(message, vo);
    }

    private String getExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        String ct = file.getContentType();
        if (ct == null) return "";
        switch (ct) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "application/pdf":
                return "pdf";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            default:
                return "";
        }
    }
}
