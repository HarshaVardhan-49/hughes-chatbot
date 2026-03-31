package com.hughes.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.docs-prefix}")
    private String docsPrefix;

    private S3Client buildClient() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public List<String> listDocuments() {
        S3Client s3 = buildClient();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(docsPrefix)
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(request);
        List<String> keys = new ArrayList<>();
        for (S3Object obj : response.contents()) {
            keys.add(obj.key());
        }
        return keys;
    }

    public String downloadDocument(String key) {
        S3Client s3 = buildClient();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        s3.getObject(request),
                        StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download: " + key, e);
        }
    }
}