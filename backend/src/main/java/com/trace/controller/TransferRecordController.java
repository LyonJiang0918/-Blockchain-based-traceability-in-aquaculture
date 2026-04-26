package com.trace.controller;

import com.trace.dto.TransferRecordDTO;
import com.trace.dto.request.CreateTransferRecordRequest;
import com.trace.service.TransferRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/transfer")
@Tag(name = "流转记录管理", description = "流转记录接口")
public class TransferRecordController {

    @Autowired
    private TransferRecordService transferRecordService;

    @PostMapping
    @Operation(summary = "创建流转记录", description = "创建新的流转记录并上链")
    public ResponseEntity<Map<String, Object>> createTransferRecord(@RequestBody CreateTransferRecordRequest request) {
        try {
            String metaHash = transferRecordService.createTransferRecord(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("metaHash", metaHash);
            result.put("recordId", request.getRecordId());
            result.put("message", "流转记录创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建流转记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "查询批次流转记录", description = "根据批次ID查询所有流转记录")
    public ResponseEntity<Map<String, Object>> listByBatchId(@PathVariable String batchId) {
        try {
            List<TransferRecordDTO> list = transferRecordService.listByBatchId(batchId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询流转记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询流转记录详情", description = "根据记录ID查询流转记录详情")
    public ResponseEntity<Map<String, Object>> getByRecordId(@PathVariable String recordId) {
        try {
            TransferRecordDTO dto = transferRecordService.getByRecordId(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询流转记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
