package com.trace.controller;

import com.trace.dto.request.CreateProcessRecordRequest;
import com.trace.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加工管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/process")
@Tag(name = "加工管理", description = "加工记录和状态管理接口")
public class ProcessController {

    @Autowired
    private ProcessService processService;

    /**
     * 创建加工记录（启动加工）
     * 将指定养殖群的动物状态改为"加工中"
     */
    @PostMapping
    @Operation(summary = "启动加工", description = "创建加工记录，将动物状态改为加工中")
    public ResponseEntity<Map<String, Object>> startProcess(
            @Validated @RequestBody CreateProcessRecordRequest request,
            HttpSession session) {
        try {
            String userRole = getUserRole(session);
            String userFarmId = getUserFarmId(session);
            Object result = processService.startProcess(request, userRole, userFarmId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("data", result);
            res.put("message", "加工已启动");
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            log.warn("启动加工失败: {}", e.getMessage());
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            log.error("启动加工失败", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    /**
     * 完成加工
     * 将加工中的记录和动物状态改为"已加工"
     */
    @PutMapping("/{recordId}/complete")
    @Operation(summary = "完成加工", description = "标记加工完成，将动物状态改为已加工")
    public ResponseEntity<Map<String, Object>> completeProcess(
            @PathVariable String recordId,
            @RequestBody(required = false) CreateProcessRecordRequest request,
            HttpSession session) {
        try {
            String userRole = getUserRole(session);
            processService.completeProcess(recordId, request, userRole);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "加工已完成");
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            log.warn("完成加工失败: {}", e.getMessage());
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            log.error("完成加工失败", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    /**
     * 查询某批次的加工记录
     */
    @GetMapping("/batch/{batchId}")
    @Operation(summary = "查询批次的加工记录")
    public ResponseEntity<Map<String, Object>> listByBatch(@PathVariable String batchId) {
        try {
            List<?> records = processService.listByBatchId(batchId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("list", records);
            res.put("total", records.size());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("查询加工记录失败", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    /**
     * 查询所有加工中记录（供加工厂使用）
     */
    @GetMapping("/processing")
    @Operation(summary = "查询加工中的记录")
    public ResponseEntity<Map<String, Object>> listProcessing(HttpSession session) {
        try {
            String userRole = getUserRole(session);
            String userFarmId = getUserFarmId(session);
            List<?> records = processService.listProcessing(userRole, userFarmId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("list", records);
            res.put("total", records.size());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("查询加工中记录失败", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    private String getUserRole(HttpSession session) {
        Object userObj = session.getAttribute("SESSION_USER");
        if (userObj == null) return null;
        try {
            Object role = userObj.getClass().getMethod("getRole").invoke(userObj);
            return role != null ? role.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取用户标识：优先 farmId，为空则用 username（FARM 用户 fallback 到 username）
     */
    private String getUserFarmId(HttpSession session) {
        Object userObj = session.getAttribute("SESSION_USER");
        if (userObj == null) return null;
        try {
            Object farmId = userObj.getClass().getMethod("getFarmId").invoke(userObj);
            if (farmId != null && !farmId.toString().trim().isEmpty()) {
                return farmId.toString().trim();
            }
            Object role = userObj.getClass().getMethod("getRole").invoke(userObj);
            if ("FARM".equals(role != null ? role.toString() : null)) {
                Object username = userObj.getClass().getMethod("getUsername").invoke(userObj);
                if (username != null) return username.toString().trim();
            }
            // PROCESS/SALES 用户也 fallback 到 username
            Object username = userObj.getClass().getMethod("getUsername").invoke(userObj);
            return username != null ? username.toString().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
