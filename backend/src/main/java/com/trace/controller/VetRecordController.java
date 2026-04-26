package com.trace.controller;

import com.trace.dto.VetRecordDTO;
import com.trace.dto.request.CreateVetRecordRequest;
import com.trace.service.VetRecordService;
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
 * 兽医/疫苗记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/vet")
@Tag(name = "兽医记录管理", description = "疫苗接种和兽医记录接口")
public class VetRecordController {

    @Autowired
    private VetRecordService vetRecordService;

    @PostMapping
    @Operation(summary = "创建兽医记录", description = "创建新的疫苗接种/用药/治疗记录")
    public ResponseEntity<Map<String, Object>> createVetRecord(
            @Validated @RequestBody CreateVetRecordRequest request) {
        try {
            String metaHash = vetRecordService.createVetRecord(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("metaHash", metaHash);
            result.put("recordId", request.getRecordId());
            result.put("message", "兽医记录创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建兽医记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "查询养殖群兽医记录", description = "根据养殖群ID查询所有兽医记录")
    public ResponseEntity<Map<String, Object>> listByGroupId(@PathVariable String groupId) {
        try {
            List<VetRecordDTO> list = vetRecordService.listByGroupId(groupId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询兽医记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/group/{groupId}/type/{recordType}")
    @Operation(summary = "按类型查询兽医记录", description = "根据养殖群ID和记录类型查询（0免疫/1用药/2治疗）")
    public ResponseEntity<Map<String, Object>> listByGroupIdAndType(
            @PathVariable String groupId,
            @PathVariable Integer recordType) {
        try {
            List<VetRecordDTO> list = vetRecordService.listByGroupIdAndType(groupId, recordType);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询兽医记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询兽医记录详情", description = "根据记录ID查询兽医记录详情")
    public ResponseEntity<Map<String, Object>> getByRecordId(@PathVariable String recordId) {
        try {
            VetRecordDTO dto = vetRecordService.getByRecordId(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询兽医记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{recordId}/void")
    @Operation(summary = "作废兽医记录", description = "将兽医记录标记为已作废")
    public ResponseEntity<Map<String, Object>> voidRecord(@PathVariable String recordId) {
        try {
            vetRecordService.voidRecord(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "记录已作废");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("作废兽医记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
