package com.trace.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Entity
@Table(name = "sys_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 50)
    private String farmId;

    /**
     * 厂商所在地理位置描述
     * 养殖场：养殖场详细地址（如「山东省济南市历下区养殖基地A区」）
     * 加工厂：加工厂详细地址（如「北京市丰台区食品工业园3号楼」）
     * 销售商：门店/仓库地址（如「上海市浦东新区世纪大道168号」）
     */
    @Column(length = 255)
    private String location;

    private String nickname;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        ADMIN,
        FARM,
        PROCESS,
        SALES
    }
}
