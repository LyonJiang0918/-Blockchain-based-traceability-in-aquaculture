package com.trace.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;

/**
 * FISCO BCOS 配置类
 */
@Slf4j
@Configuration
public class FiscoConfig {

    @Bean
    @ConfigurationProperties(prefix = "fisco")
    public FiscoProperties fiscoProperties() {
        return new FiscoProperties();
    }

    @Bean
    public Client fiscoClient(FiscoProperties properties) {
        if (Boolean.TRUE.equals(properties.getMockMode())) {
            log.info("使用模拟模式，无需 FISCO BCOS 节点");
            return null;
        }

        try {
            // 获取配置文件路径
            String configPath = properties.getCertPath();
            File configFile = null;

            // 处理 classpath: 前缀，转换为实际文件路径
            if (configPath.startsWith("classpath:")) {
                String relativePath = configPath.replace("classpath:", "");
                // 尝试 target/classes 目录（Spring Boot 编译后的输出位置）
                File targetFile = new File("target/classes/" + relativePath);
                if (!targetFile.exists()) {
                    // 尝试 src/main/resources 目录
                    targetFile = new File("src/main/resources/" + relativePath);
                }
                if (!targetFile.exists()) {
                    // 尝试当前工作目录
                    targetFile = new File(relativePath);
                }
                if (!targetFile.exists()) {
                    // 从 classpath 获取资源
                    java.net.URL resourceUrl = getClass().getClassLoader().getResource(relativePath);
                    if (resourceUrl != null) {
                        String decodedPath = java.net.URLDecoder.decode(resourceUrl.getFile(), "UTF-8");
                        targetFile = new File(decodedPath);
                    }
                }
                configFile = targetFile;
            } else {
                configFile = new File(configPath);
            }

            if (!configFile.exists()) {
                log.error("找不到配置文件: {}", configPath);
                return null;
            }

            // 获取 config.toml 所在的目录
            File configDir = configFile.getParentFile();
            log.info("配置文件目录: {}", configDir.getAbsolutePath());
            log.info("尝试连接 FISCO BCOS，配置文件: {}", configFile.getAbsolutePath());

            // 将 config.toml 中的 certPath 设置为相对于 config.toml 的路径
            // 由于 SDK 会从 config.toml 所在目录解析 certPath，
            // 我们需要在 config.toml 中设置正确的相对路径

            // 使用 BcosSDK.build() 方法从 TOML 配置文件初始化
            BcosSDK sdk = BcosSDK.build(configFile.getAbsolutePath());

            String groupId = "group" + properties.getGroupId();
            Client client = sdk.getClient(groupId);

            log.info("FISCO BCOS 连接成功，群组: {}", groupId);
            return client;
        } catch (Exception e) {
            log.error("FISCO BCOS 连接失败: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Data
    public static class FiscoProperties {
        private Integer groupId = 0;
        private Integer chainId = 1;
        private String certPath = "classpath:fisco/config.toml";
        private List<String> peers;
        private Boolean mockMode = false;
    }
}
