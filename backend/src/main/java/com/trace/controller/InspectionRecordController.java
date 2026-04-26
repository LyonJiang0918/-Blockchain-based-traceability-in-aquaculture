package com.trace.controller;

import com.trace.dto.InspectionRecordDTO;
import com.trace.dto.request.CreateInspectionRecordRequest;
import com.trace.service.InspectionRecordService;
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

@Slf4j
@RestController
@RequestMapping("/inspection")
@Tag(name = "检验记录管理", description = "检验记录接口")
public class InspectionRecordController {

    @Autowired
    private InspectionRecordService inspectionRecordService;

    @PostMapping
    @Operation(summary = "创建检验记录", description = "创建新的检验记录并上链")
    public ResponseEntity<Map<String, Object>> createInspectionRecord(
            @Validated @RequestBody CreateInspectionRecordRequest request) {
        try {
            String metaHash = inspectionRecordService.createInspectionRecord(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("metaHash", metaHash);
            result.put("recordId", request.getRecordId());
            result.put("message", "检验记录创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建检验记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "查询批次检验记录", description = "根据批次ID查询所有检验记录")
    public ResponseEntity<Map<String, Object>> listByBatchId(@PathVariable String batchId) {
        try {
            List<InspectionRecordDTO> list = inspectionRecordService.listByBatchId(batchId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询检验记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询检验记录详情", description = "根据记录ID查询检验记录详情")
    public ResponseEntity<Map<String, Object>> getByRecordId(@PathVariable String recordId) {
        try {
            InspectionRecordDTO dto = inspectionRecordService.getByRecordId(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询检验记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
