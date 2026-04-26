package com.trace.controller;

import com.trace.dto.ByProductDTO;
import com.trace.dto.request.CreateByProductRequest;
import com.trace.service.ByProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 农副产品控制器
 */
@Slf4j
@RestController
@RequestMapping("/byproduct")
@Tag(name = "农副产品管理", description = "农副产品创建和查询接口")
public class ByProductController {

    @Autowired
    private ByProductService byProductService;

    @PostMapping
    @Operation(summary = "创建农副产品", description = "创建新的农副产品记录")
    public ResponseEntity<Map<String, Object>> createByProduct(
            @Validated @RequestBody CreateByProductRequest request) {
        try {
            String metaHash = byProductService.createByProduct(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("metaHash", metaHash);
            result.put("productId", request.getProductId());
            result.put("message", "农副产品创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建农副产品失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "查询养殖群副产品", description = "根据养殖群ID查询所有副产品")
    public ResponseEntity<Map<String, Object>> listByGroupId(@PathVariable String groupId) {
        try {
            List<ByProductDTO> list = byProductService.listByGroupId(groupId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询副产品失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{productId}")
    @Operation(summary = "查询副产品详情", description = "根据副产品ID查询详细信息")
    public ResponseEntity<Map<String, Object>> getByProduct(@PathVariable String productId) {
        try {
            ByProductDTO dto = byProductService.getByProduct(productId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询副产品失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{productId}/status")
    @Operation(summary = "更新副产品状态", description = "更新副产品状态（库存/已销售/已使用/过期）")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String productId,
            @RequestParam Integer status) {
        try {
            byProductService.updateStatus(productId, status);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "状态更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("更新副产品状态失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
