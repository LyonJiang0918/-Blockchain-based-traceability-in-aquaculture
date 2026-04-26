package com.trace.dao;

import com.trace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(User.Role role);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 按业务编号查（养殖场/加工厂/销售商在 sys_user.farm_id 中存的编号） */
    List<User> findByFarmId(String farmId);
}
