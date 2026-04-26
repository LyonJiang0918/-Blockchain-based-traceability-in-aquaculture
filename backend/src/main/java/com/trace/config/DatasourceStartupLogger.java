package com.trace.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动后打印实际使用的数据源，便于与 Navicat 对比（多 MySQL、错端口旧进程时常见「库是空的」）。
 */
@Slf4j
@Component
public class DatasourceStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String profiles = String.join(",", env.getActiveProfiles());
        if (profiles.isEmpty()) {
            profiles = "default（未指定 profile，主要读 application.yml）";
        }
        String url = env.getProperty("spring.datasource.url", "(未配置)");
        String user = env.getProperty("spring.datasource.username", "(未配置)");
        log.info("================================================================");
        log.info("【数据源自检】请对照 Navicat：只有 JDBC URL 里的库名一致，数据才会写进你看到的库。");
        log.info("  激活 profile: {}", profiles);
        log.info("  JDBC URL   : {}", url);
        log.info("  用户名     : {}", user);
        log.info("  端口占用提示: 若 8081 上仍是旧进程，可能连的是旧配置（例如 H2），请关掉旧 Java 再启动。");
        log.info("================================================================");
    }
}
