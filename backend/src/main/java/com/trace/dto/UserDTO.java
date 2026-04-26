package com.trace.dto;

import com.trace.entity.User;
import lombok.Data;

/**
 * 用户数据传输对象（不包含密码）
 */
@Data
public class UserDTO {

    private Long id;
    private String username;
    private String role;
    private String farmId;
    private String nickname;
    private String location;
    private Long createdAt;
    private Long lastLoginAt;

    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setFarmId(user.getFarmId());
        dto.setNickname(user.getNickname());
        dto.setLocation(user.getLocation());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toEpochSecond(java.time.ZoneOffset.of("+8")) : null);
        dto.setLastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toEpochSecond(java.time.ZoneOffset.of("+8")) : null);
        return dto;
    }
}
