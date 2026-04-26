package com.trace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 合约地址配置
 */
@Configuration
@ConfigurationProperties(prefix = "contract")
@Data
public class ContractConfig {
    private String batchRegistry;
    private String feedRecord;
    private String vetRecord;
    private String inspectionRecord;
    private String transferRecord;
}



