package com.lyz.util;

import com.lyz.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioUtil {
    private final MinioConfig minioConfig;
    private final MinioClient minioClient;


    /** 确保桶存在（项目启动时调用一次） */
    @PostConstruct
    public void initBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
            // 设置公开读策略
            String policy = "{\n" +
                    "  \"Version\": \"2012-10-17\",\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
                    "      \"Action\": [\"s3:GetObject\"],\n" +
                    "      \"Resource\": [\"arn:aws:s3:::" + minioConfig.getBucket() + "/*\"]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .config(policy)
                            .build()
            );
            log.info("Bucket {} created.", minioConfig.getBucket());
        }
    }

    //上传文件
    public String uploadFile(MultipartFile file,String prefix, String objectName) throws Exception{
        String fulPath = prefix + "/" + objectName;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(fulPath)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        log.info("Upload success: {}/{}", minioConfig.getBucket(), fulPath);
        return fulPath;
    }

    // 获取固定URL的方法
    public String getPublicUrl(String objectName) {
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
    }



}
