package org.skinAI.services;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyuncs.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;

@Service
public class OssService {

    private static final String DEFAULT_PREFIX = "skinAI/";

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucketname}")
    private String bucketName;

    @Value("${aliyun.oss.region}")
    private String region;

    public String generatePresignedUrl(String objectKey) {
        OSS ossClient = createClient();
        try {
            String normalizedKey = resolveExistingKey(ossClient, objectKey);
            Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000);
            return ossClient.generatePresignedUrl(bucketName, normalizedKey, expiration).toString();
        } finally {
            ossClient.shutdown();
        }
    }

    public String uploadFile(String objectKey, InputStream in) throws Exception {
        String normalizedKey = normalizeObjectKey(objectKey);
        OSS ossClient = createClient();
        try {
            ossClient.putObject(new PutObjectRequest(bucketName, normalizedKey, in));
            return normalizedKey;
        } finally {
            ossClient.shutdown();
        }
    }

    public void deleteFile(String objectKey) {
        OSS ossClient = createClient();
        try {
            String normalizedKey = normalizeObjectKey(objectKey);
            if (ossClient.doesObjectExist(bucketName, normalizedKey)) {
                ossClient.deleteObject(bucketName, normalizedKey);
            }
            for (String legacyKey : buildLegacyKeys(normalizedKey)) {
                if (ossClient.doesObjectExist(bucketName, legacyKey)) {
                    ossClient.deleteObject(bucketName, legacyKey);
                }
            }
        } finally {
            ossClient.shutdown();
        }
    }

    public InputStream getObjectStream(String objectKey) {
        OSS ossClient = createClient();
        try {
            String normalizedKey = resolveExistingKey(ossClient, objectKey);
            OSSObject object = ossClient.getObject(bucketName, normalizedKey);
            return new AutoCloseableInputStream(object.getObjectContent(), ossClient);
        } catch (Exception ex) {
            ossClient.shutdown();
            throw ex;
        }
    }

    public String normalizeObjectKey(String objectKey) {
        if (objectKey == null) {
            return "";
        }

        String key = objectKey.trim().replaceAll("^\"|\"$", "");
        if (key.isEmpty()) {
            return "";
        }

        if (key.startsWith("http://") || key.startsWith("https://")) {
            try {
                URI uri = URI.create(key);
                key = uri.getPath();
            } catch (Exception ignored) {
                return key;
            }
        }

        key = key.replaceFirst("^/+", "");
        if (key.startsWith(bucketName + "/")) {
            key = key.substring(bucketName.length() + 1);
        }
        if (!key.contains("/")) {
            key = DEFAULT_PREFIX + key;
        }
        return key;
    }

    private OSS createClient() {
        EnvironmentVariableCredentialsProvider credentialsProvider;
        try {
            credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        } catch (ClientException e) {
            throw new RuntimeException("Failed to create OSS credentials provider", e);
        }

        ClientBuilderConfiguration config = new ClientBuilderConfiguration();
        config.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(config)
                .region(region)
                .build();
    }

    private String resolveExistingKey(OSS ossClient, String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        if (ossClient.doesObjectExist(bucketName, normalizedKey)) {
            return normalizedKey;
        }
        for (String legacyKey : buildLegacyKeys(normalizedKey)) {
            if (ossClient.doesObjectExist(bucketName, legacyKey)) {
                return legacyKey;
            }
        }
        return normalizedKey;
    }

    private String[] buildLegacyKeys(String normalizedKey) {
        String filename = normalizedKey.replaceFirst("^skinAI/", "").replaceFirst("^skin/", "");
        return new String[]{"skin/" + filename, "skinAI/" + filename};
    }

    private static class AutoCloseableInputStream extends InputStream {
        private final InputStream delegate;
        private final OSS client;

        private AutoCloseableInputStream(InputStream delegate, OSS client) {
            this.delegate = delegate;
            this.client = client;
        }

        @Override
        public int read() throws java.io.IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b) throws java.io.IOException {
            return delegate.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws java.io.IOException {
            try {
                delegate.close();
            } finally {
                client.shutdown();
            }
        }
    }
}
