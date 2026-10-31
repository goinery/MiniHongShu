package com.hongshu.common.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/** 阿里云 OSS 配置；凭证只从后端运行环境读取。 */
@Getter
@Configuration
public class OssConfig {
    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.region}")
    private String region;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${oss.public-url:}")
    private String publicUrl;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        Assert.hasText(endpoint, "请配置 OSS_ENDPOINT");
        Assert.hasText(region, "请配置 OSS_REGION");
        Assert.hasText(bucket, "请配置 OSS_BUCKET");
        Assert.hasText(accessKeyId, "请配置 OSS_ACCESS_KEY_ID");
        Assert.hasText(accessKeySecret, "请配置 OSS_ACCESS_KEY_SECRET");
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setSignatureVersion(SignVersion.V4);
        configuration.setConnectionTimeout(10_000);
        configuration.setSocketTimeout(30_000);
        configuration.setConnectionRequestTimeout(10_000);
        configuration.setMaxErrorRetry(2);
        return OSSClientBuilder.create()
                .endpoint(endpoint)
                .region(region)
                .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
                .clientConfiguration(configuration)
                .build();
    }
}
