package com.trace.controller;

import com.trace.dto.BatchDTO;
import com.trace.dto.UserDTO;
import com.trace.dto.request.CreateBatchRequest;
import com.trace.service.BatchService;
import com.trace.service.UserService;
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
import java.util.stream.Collectors;

/**
 * 批次管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/batch")
@Tag(name = "批次管理", description = "批次创建和查询接口")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @Autowired
    private UserService userService;

    /**
     * 统一从会话提取 role + effective farmId。
     * FARM 用户：farmId 为空时用 username 兜底
     * PROCESS/SALES 用户：直接用 username 作为标识（farmId 通常为空）
     */
    private void extractSessionUser(HttpSession session, java.util.Map<String, String> out) {
        out.put("role", null);
        out.put("farmId", null);
        Object userObj = session.getAttribute("SESSION_USER");
        if (userObj == null) return;
        try {
            Object roleField = userObj.getClass().getMethod("getRole").invoke(userObj);
            if (roleField != null) out.put("role", roleField.toString());

            Object farmIdField = userObj.getClass().getMethod("getFarmId").invoke(userObj);
            String fid = (farmIdField != null && !farmIdField.toString().trim().isEmpty())
                    ? farmIdField.toString().trim() : null;

            // FARM 用户如果 farmId 为空，用 username 作为标识
            if (fid == null && "FARM".equals(out.get("role"))) {
                Object usernameField = userObj.getClass().getMethod("getUsername").invoke(userObj);
                if (usernameField != null) fid = usernameField.toString().trim();
            }

            // PROCESS/SALES 用户：直接用 username 作为标识（farmId 通常为空）
            if (fid == null && ("PROCESS".equals(out.get("role")) || "SALES".equals(out.get("role")))) {
                Object usernameField = userObj.getClass().getMethod("getUsername").invoke(userObj);
                if (usernameField != null) fid = usernameField.toString().trim();
            }

            out.put("farmId", fid);
        } catch (Exception ignored) {}
    }

    @PostMapping
    @Operation(summary = "创建批次", description = "创建新的养殖批次并上链")
    public ResponseEntity<Map<String, Object>> createBatch(
            @Validated @RequestBody CreateBatchRequest request,
            HttpSession session) {
        try {
            applyFarmIdForFarmUserOnCreate(request, session);
            validateAdminFarmSelection(request, session);
            String txHash = batchService.createBatch(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("txHash", txHash);
            result.put("batchId", request.getBatchId());
            result.put("message", "批次创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建批次失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 养殖场账号创建批次时，强制使用会话中的养殖场编号（防止前端残留或篡改）。
     * 优先 user.farmId，为空则回退为 username。
     */
    private void applyFarmIdForFarmUserOnCreate(CreateBatchRequest request, HttpSession session) {
        Object attr = session.getAttribute("SESSION_USER");
        if (!(attr instanceof UserDTO)) {
            return;
        }
        UserDTO u = (UserDTO) attr;
        if (!"FARM".equals(u.getRole())) {
            return;
        }
        String effective = u.getFarmId();
        if (effective == null || effective.trim().isEmpty()) {
            effective = u.getUsername();
        } else {
            effective = effective.trim();
        }
        if (effective == null || effective.isEmpty()) {
            throw new RuntimeException("养殖场账号未绑定养殖场编号，无法创建养殖群");
        }
        request.setFarmId(effective);
    }

    /**
     * 管理员代建时：farmId 必须来自已注册的养殖场账号（防止随意填写编号）。
     */
    private void validateAdminFarmSelection(CreateBatchRequest request, HttpSession session) {
        Object attr = session.getAttribute("SESSION_USER");
        if (!(attr instanceof UserDTO)) {
            return;
        }
        UserDTO u = (UserDTO) attr;
        if (!"ADMIN".equals(u.getRole())) {
            return;
        }
        String fid = request.getFarmId();
        if (fid == null || fid.trim().isEmpty()) {
            throw new RuntimeException("请选择已注册的养殖场");
        }
        if (!userService.isRegisteredFarmBusinessId(fid.trim())) {
            throw new RuntimeException("养殖场编号无效：请从列表中选择已在系统中注册的养殖场");
        }
    }

    /**
     * 作废单个养殖群（区块链特性：数据不可删除，只能作废标记）
     */
    @PutMapping("/{groupId}/invalidate")
    @Operation(summary = "作废养殖群", description = "将指定的养殖群标记为作废（区块链不可删除特性）")
    public ResponseEntity<Map<String, Object>> invalidateBatch(
            @PathVariable String groupId,
            @RequestParam(required = false) String reason,
            HttpSession session) {
        try {
            Object userObj = session.getAttribute("SESSION_USER");
            if (userObj == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "请先登录");
                return ResponseEntity.status(401).body(result);
            }
            String role = null;
            String operatorId = null;
            try {
                Object roleField = userObj.getClass().getMethod("getRole").invoke(userObj);
                if (roleField != null) role = roleField.toString();
                Object usernameField = userObj.getClass().getMethod("getUsername").invoke(userObj);
                if (usernameField != null) operatorId = usernameField.toString();
            } catch (Exception ignored) {}

            if (!"ADMIN".equals(role)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "只有管理员可以执行此操作");
                return ResponseEntity.status(403).body(result);
            }

            String reasonFinal = (reason != null && !reason.isEmpty()) ? reason : "管理员手动作废";
            batchService.invalidateBatch(groupId, reasonFinal, operatorId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "养殖群已作废（数据已上链，不可删除）");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("作废养殖群失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 作废所有养殖群（管理员专用）
     * 区块链特性：数据不可删除，只能全部标记为作废
     */
    @PutMapping("/all/invalidate")
    @Operation(summary = "作废所有养殖群", description = "将所有养殖群标记为作废（区块链不可删除特性）")
    public ResponseEntity<Map<String, Object>> invalidateAllBatches(
            @RequestParam(required = false) String reason,
            HttpSession session) {
        try {
            Object userObj = session.getAttribute("SESSION_USER");
            if (userObj == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "请先登录");
                return ResponseEntity.status(401).body(result);
            }
            String role = null;
            String operatorId = null;
            try {
                Object roleField = userObj.getClass().getMethod("getRole").invoke(userObj);
                if (roleField != null) role = roleField.toString();
                Object usernameField = userObj.getClass().getMethod("getUsername").invoke(userObj);
                if (usernameField != null) operatorId = usernameField.toString();
            } catch (Exception ignored) {}

            if (!"ADMIN".equals(role)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "只有管理员可以执行此操作");
                return ResponseEntity.status(403).body(result);
            }

            String reasonFinal = (reason != null && !reason.isEmpty()) ? reason : "管理员批量作废";
            batchService.invalidateAllBatches(reasonFinal, operatorId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "所有养殖群已作废（区块链数据不可删除，仅标记作废）");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("批量作废失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping
    @Operation(summary = "查询批次列表", description = "查询所有批次信息，支持按状态筛选")
    public ResponseEntity<Map<String, Object>> listBatches(
            @RequestParam(required = false) Integer status,
            HttpSession session) {
        try {
            Map<String, String> ctx = new java.util.HashMap<>();
            extractSessionUser(session, ctx);
            List<BatchDTO> list = batchService.listBatches(status, ctx.get("role"), ctx.get("farmId"));
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            result.put("userRole", ctx.get("role"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询批次列表失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "查询养殖群", description = "根据养殖群ID查询养殖群信息")
    public ResponseEntity<Map<String, Object>> getBatch(@PathVariable String groupId) {
        try {
            BatchDTO batch = batchService.getBatch(groupId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", batch);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询养殖群失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{groupId}/status")
    @Operation(summary = "更新养殖群状态", description = "更新养殖群的状态（带角色权限校验）")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String groupId,
            @RequestParam Integer status,
            @RequestParam(required = false) String targetProcessId,
            @RequestParam(required = false) String targetSalesId,
            HttpSession session) {
        try {
            Map<String, String> ctx = new java.util.HashMap<>();
            extractSessionUser(session, ctx);
            String txHash = batchService.updateBatchStatus(groupId, status, ctx.get("role"), ctx.get("farmId"), targetProcessId, targetSalesId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("txHash", txHash);
            result.put("message", "状态更新成功");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("更新养殖群状态失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("更新养殖群状态失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{groupId}/return")
    @Operation(summary = "返回在栏", description = "撤回出栏操作，返回在养状态（前提：加工厂未加工完成）")
    public ResponseEntity<Map<String, Object>> returnToFarm(
            @PathVariable String groupId,
            HttpSession session) {
        try {
            Map<String, String> ctx = new java.util.HashMap<>();
            extractSessionUser(session, ctx);
            String txHash = batchService.returnToFarm(groupId, ctx.get("role"), ctx.get("farmId"));
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("txHash", txHash);
            result.put("message", "已成功撤回至在养状态");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("撤回失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("撤回失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/trace/{groupId}")
    @Operation(summary = "消费者溯源查询（无需登录）", description = "通过输入养殖群ID或扫描二维码查询完整的溯源信息")
    public ResponseEntity<Map<String, Object>> traceQuery(@PathVariable String groupId) {
        try {
            BatchDTO batch = batchService.getBatch(groupId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", batch);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("溯源查询失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "未找到该养殖群信息，请确认ID是否正确");
            return ResponseEntity.badRequest().body(result);
        }
    }
}
