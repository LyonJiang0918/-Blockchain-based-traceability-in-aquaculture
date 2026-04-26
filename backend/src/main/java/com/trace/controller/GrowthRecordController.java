package com.trace.controller;

import com.trace.dto.GrowthRecordDTO;
import com.trace.dto.request.CreateGrowthRecordRequest;
import com.trace.service.GrowthRecordService;
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
 * 成长记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/growth")
@Tag(name = "成长记录管理", description = "养殖群成长记录接口")
public class GrowthRecordController {

    @Autowired
    private GrowthRecordService growthRecordService;

    @PostMapping
    @Operation(summary = "创建成长记录", description = "创建新的成长记录（体重、健康状态等）")
    public ResponseEntity<Map<String, Object>> createGrowthRecord(
            @Validated @RequestBody CreateGrowthRecordRequest request) {
        try {
            String metaHash = growthRecordService.createGrowthRecord(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("metaHash", metaHash);
            result.put("recordId", request.getRecordId());
            result.put("message", "成长记录创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建成长记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "查询养殖群成长记录", description = "根据养殖群ID查询所有成长记录")
    public ResponseEntity<Map<String, Object>> listByGroupId(@PathVariable String groupId) {
        try {
            List<GrowthRecordDTO> list = growthRecordService.listByGroupId(groupId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询成长记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询成长记录详情", description = "根据记录ID查询成长记录详情")
    public ResponseEntity<Map<String, Object>> getByRecordId(@PathVariable String recordId) {
        try {
            GrowthRecordDTO dto = growthRecordService.getByRecordId(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询成长记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{recordId}/void")
    @Operation(summary = "作废成长记录", description = "将成长记录标记为已作废")
    public ResponseEntity<Map<String, Object>> voidRecord(@PathVariable String recordId) {
        try {
            growthRecordService.voidRecord(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "记录已作废");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("作废成长记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
