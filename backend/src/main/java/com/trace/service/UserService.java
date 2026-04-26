package com.trace.service;

import com.trace.dao.UserRepository;
import com.trace.dto.UserDTO;
import com.trace.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return UserDTO.fromEntity(user);
    }

    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public List<UserDTO> listUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色获取用户列表（用于选择加工厂或销售商）
     * @param role PROCESS 或 SALES
     */
    public List<UserDTO> listUsersByRole(String role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals(role))
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 与前端 getEffectiveFarmId / 会话逻辑一致：有 farmId 用 farmId，否则用 username。
     */
    public static String effectiveFarmBusinessId(User u) {
        if (u.getFarmId() != null && !u.getFarmId().trim().isEmpty()) {
            return u.getFarmId().trim();
        }
        return u.getUsername();
    }

    /** 管理员代建养殖群时：提交的养殖场编号必须对应已注册的养殖场账号 */
    public boolean isRegisteredFarmBusinessId(String farmId) {
        if (farmId == null || farmId.trim().isEmpty()) {
            return false;
        }
        String target = farmId.trim();
        return userRepository.findByRole(User.Role.FARM).stream()
                .anyMatch(u -> effectiveFarmBusinessId(u).equals(target));
    }

    @Transactional
    public UserDTO createUser(String username, String password, String role, String farmId, String nickname, String location) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.valueOf(role));
        user.setFarmId(farmId);
        user.setNickname(nickname);
        user.setLocation(location);
        return UserDTO.fromEntity(userRepository.save(user));
    }

    /**
     * 更新用户信息（管理员可编辑所有用户）
     * @param id 用户ID
     * @param nickname 昵称
     * @param farmId 养殖场编号（仅养殖场角色可修改）
     * @param location 地理位置描述
     */
    @Transactional
    public UserDTO updateUser(Long id, String nickname, String farmId, String location) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setNickname(nickname);
        if ("FARM".equals(user.getRole().name())) {
            user.setFarmId(farmId);
        }
        user.setLocation(location);
        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
