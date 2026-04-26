package com.trace.config;

import com.trace.dao.UserRepository;
import com.trace.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 启动时插入演示账号（如果不存在）
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 演示账号配置
        List<DemoUser> demoUsers = Arrays.asList(
            new DemoUser("admin", "admin", "ADMIN", null, null),
            new DemoUser("farm001", "123456", "FARM", "FARM001", "山东省济南市历下区养殖基地A区"),
            new DemoUser("process001", "123456", "PROCESS", null, "北京市丰台区食品工业园3号楼"),
            new DemoUser("sales001", "123456", "SALES", null, "上海市浦东新区世纪大道168号永辉超市")
        );

        for (DemoUser du : demoUsers) {
            if (!userRepository.findByUsername(du.username).isPresent()) {
                User user = new User();
                user.setUsername(du.username);
                user.setPassword(passwordEncoder.encode(du.password));
                user.setRole(User.Role.valueOf(du.role));
                user.setFarmId(du.farmId);
                user.setNickname(du.nickname);
                user.setLocation(du.location);
                user.setCreatedAt(LocalDateTime.now());
                userRepository.save(user);
                log.info("创建演示账号: {} / {} / {} / 地址: {}", du.username, du.password, du.role, du.location);
            } else {
                // 账号已存在但无地址，补全演示地址
                userRepository.findByUsername(du.username).ifPresent(existing -> {
                    if ((existing.getLocation() == null || existing.getLocation().isEmpty()) && du.location != null) {
                        existing.setLocation(du.location);
                        userRepository.save(existing);
                        log.info("补全演示账号地址: {} -> {}", du.username, du.location);
                    }
                });
            }
        }
    }

    private static class DemoUser {
        String username;
        String password;
        String role;
        String farmId;
        String location;
        String nickname;

        DemoUser(String username, String password, String role, String farmId, String location) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.farmId = farmId;
            this.location = location;
            this.nickname = roleName(role) + " - " + username;
        }

        private static String roleName(String role) {
            switch (role) {
                case "ADMIN": return "管理员";
                case "FARM": return "养殖场";
                case "PROCESS": return "加工厂";
                case "SALES": return "销售商";
                default: return role;
            }
        }
    }
}
