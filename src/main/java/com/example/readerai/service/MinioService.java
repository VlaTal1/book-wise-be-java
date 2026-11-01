package com.example.readerai.service;

import com.example.readerai.dto.FileInfo;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing MinIO bucket", e);
        }
    }

    public FileInfo uploadFile(MultipartFile file) throws IOException, MinioException {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;

            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            String filePath = "/" + bucketName + "/" + filename;

            return FileInfo.builder()
                    .fileName(filename)
                    .originalFileName(originalFilename)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .fileType(contentType)
                    .build();

        } catch (Exception e) {
            throw new MinioException("Error uploading file to MinIO: " + e.getMessage());
        }
    }

    // Розмір об'єкта в бакеті або null, якщо його немає.
    public Long fileSize(String filename) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(filename).build()).size();
        } catch (Exception e) {
            return null;
        }
    }

    public void uploadStream(String filename, InputStream stream, long size, String contentType) throws MinioException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("Error uploading file to MinIO: " + e.getMessage());
        }
    }

    public InputStream getFile(String filename) throws MinioException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("Error getting file from MinIO: " + e.getMessage());
        }
    }

    public void deleteFile(String filename) throws MinioException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("Error deleting file from MinIO: " + e.getMessage());
        }
    }

    public List<String> listFiles() throws MinioException {
        List<String> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                files.add(item.objectName());
            }
            return files;
        } catch (Exception e) {
            throw new MinioException("Error listing files from MinIO: " + e.getMessage());
        }
    }
}