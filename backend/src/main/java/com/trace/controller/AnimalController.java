package com.trace.controller;

import com.trace.dto.request.CreateAnimalRequest;
import com.trace.service.AnimalService;
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
 * 单体动物管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/animal")
@Tag(name = "动物管理", description = "单体动物（耳标）登记和查询接口")
public class AnimalController {

    @Autowired
    private AnimalService animalService;

    /**
     * 入栏登记：从养殖群拆分登记单体动物
     */
    @PostMapping
    @Operation(summary = "登记单体动物", description = "将养殖群中的动物以耳标形式逐个登记入栏")
    public ResponseEntity<Map<String, Object>> registerAnimal(
            @Validated @RequestBody CreateAnimalRequest request,
            HttpSession session) {
        try {
            String userRole = getUserRole(session);
            String userFarmId = getUserFarmId(session);
            String txHash = animalService.registerAnimal(request, userRole, userFarmId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("txHash", txHash);
            result.put("animalId", request.getAnimalId());
            result.put("message", "动物登记成功");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("登记动物失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("登记动物失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 批量登记动物
     */
    @PostMapping("/batch")
    @Operation(summary = "批量登记动物", description = "一次性登记多只动物到同一养殖群")
    public ResponseEntity<Map<String, Object>> registerAnimalsBatch(
            @RequestBody List<CreateAnimalRequest> requests,
            HttpSession session) {
        try {
            String userRole = getUserRole(session);
            String userFarmId = getUserFarmId(session);
            List<String> animalIds = animalService.registerAnimalsBatch(requests, userRole, userFarmId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", animalIds.size());
            result.put("animalIds", animalIds);
            result.put("message", "批量登记成功，共 " + animalIds.size() + " 只");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("批量登记动物失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("批量登记动物失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 查询某养殖群的所有动物
     */
    @GetMapping("/batch/{batchId}")
    @Operation(summary = "查询养殖群的动物列表")
    public ResponseEntity<Map<String, Object>> listByBatch(@PathVariable String batchId) {
        try {
            List<?> animals = animalService.listByBatchId(batchId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", animals);
            result.put("total", animals.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询动物列表失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 按耳标号精确查询
     */
    @GetMapping("/{animalId}")
    @Operation(summary = "按耳标号查询动物")
    public ResponseEntity<Map<String, Object>> getByAnimalId(@PathVariable String animalId) {
        try {
            Object animal = animalService.getByAnimalId(animalId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", animal);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询动物失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
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
            Object username = userObj.getClass().getMethod("getUsername").invoke(userObj);
            return username != null ? username.toString().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
