package com.trace.dto.trace;

import com.trace.entity.User;
import lombok.Data;

/**
 * 溯源页展示的参与方信息（不含密码等敏感字段）
 */
@Data
public class TraceParticipantDTO {

    /** FARM / PROCESS / SALES */
    private String role;

    /** 角色中文说明 */
    private String roleLabel;

    /** 登录账号 */
    private String username;

    /** 展示名称（昵称优先） */
    private String displayName;

    /**
     * 业务侧编号：养殖场编号 / 加工厂编号 / 销售商编号（与 sys_user.farm_id 一致）
     */
    private String businessId;

    /**
     * 经营地址（与 sys_user.location 一致：养殖场地址、加工厂地址、销售商门店地址）
     */
    private String address;

    public static TraceParticipantDTO fromUser(User u, String roleLabel) {
        if (u == null) {
            return null;
        }
        TraceParticipantDTO d = new TraceParticipantDTO();
        d.setRole(u.getRole() != null ? u.getRole().name() : null);
        d.setRoleLabel(roleLabel);
        d.setUsername(u.getUsername());
        d.setDisplayName(u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername());
        d.setBusinessId(u.getFarmId());
        d.setAddress(u.getLocation());
        return d;
    }
}
