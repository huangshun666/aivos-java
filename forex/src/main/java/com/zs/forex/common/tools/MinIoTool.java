package com.zs.forex.common.tools;

import com.zs.forex.common.config.MinioConfig;
import io.minio.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Minio工具类
 */
@Slf4j
@Component
public class MinIoTool {

    public static MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @PostConstruct
    public void init() {

        //创建一个MinIO的Java客户端
        minioClient = MinioClient.builder()
                // minio服务端地址URL
                .endpoint(minioConfig.getEndpoint())
                // 用户名及密码（访问密钥/密钥）
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();
        try {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!isExist) {
                //创建存储桶并设置只读权限
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build());
                createBucketPolicy(minioConfig.getBucketName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传
     *
     * @Author tencreat
     * @Date: 2022/03/10 09:55
     */
    public String uploadMinio(MultipartFile file) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            // 设置存储对象名称
            String objectName = sdf.format(new Date()) + "/" + UUID.randomUUID() +
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            // 使用putObject上传一个文件到存储桶中
            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType(contentType)
                    .build());

            inputStream.close();
            return minioConfig.getOpenAddr() + "/" + minioConfig.getBucketName() + "/" + objectName;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("上传发生错误: {}！", e.getMessage());

        }
        // 上传失败
        return " ";
    }

    @SneakyThrows(Exception.class)
    public void createBucketPolicy(String bucketName) {
        StringBuilder builder = defaultBucketPolicy(bucketName);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(builder.toString())
                .build());
    }

    /**
     * 获取默认桶策略(只读)
     *
     * @param bucketName
     * @return
     */
    private static StringBuilder defaultBucketPolicy(String bucketName) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n" +
                "    \"Version\": \"2012-10-17\",\n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Principal\": \"*\",\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Action\": [\n" +
                "                \"s3:GetBucketLocation\",\n" +
                "                \"s3:GetObject\"\n" +
                "            ],\n" +
                "            \"Resource\": [\n" +
                "                \"arn:aws:s3:::*\"\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}");
        return builder;
    }

}
