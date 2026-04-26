package com.trace.controller;

import com.trace.dto.UserDTO;
import com.trace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户登录、认证和账号管理接口")
public class UserController {

    private static final String SESSION_USER = "SESSION_USER";

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名+密码登录，写入Session")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {
        try {
            UserDTO user = userService.login(username, password);
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_USER, user);

            // 同步写入 Spring Security 上下文，否则 anyRequest().authenticated() 始终视为未登录
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", user);
            result.put("message", "登录成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("登录失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已退出登录");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute(SESSION_USER);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "未登录");
            return ResponseEntity.status(401).body(result);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check")
    @Operation(summary = "检查登录状态")
    public ResponseEntity<Map<String, Object>> checkLogin(HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute(SESSION_USER);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("loggedIn", user != null);
        if (user != null) {
            result.put("data", user);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "查询用户列表（仅管理员）")
    public ResponseEntity<Map<String, Object>> listUsers() {
        try {
            List<UserDTO> list = userService.listUsers();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/by-role/{role}")
    @Operation(summary = "根据角色查询用户列表", description = "获取指定角色的用户列表，用于选择加工厂或销售商")
    public ResponseEntity<Map<String, Object>> listUsersByRole(@PathVariable String role) {
        try {
            List<UserDTO> list = userService.listUsersByRole(role);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping
    @Operation(summary = "创建用户（仅管理员）")
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) String farmId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String location) {
        try {
            UserDTO user = userService.createUser(username, password, role, farmId, nickname, location);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", user);
            result.put("message", "用户创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息（管理员可编辑）", description = "更新用户昵称、养殖场编号、地理位置")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String farmId,
            @RequestParam(required = false) String location) {
        try {
            UserDTO user = userService.updateUser(id, nickname, farmId, location);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", user);
            result.put("message", "更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "公开注册接口，无需登录")
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) String farmId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String location) {
        try {
            UserDTO user = userService.createUser(username, password, role, farmId, nickname, location);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", user);
            result.put("message", "注册成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户（仅管理员）")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
