package com.trace.controller;

import com.trace.dto.FeedRecordDTO;
import com.trace.dto.request.CreateFeedRecordRequest;
import com.trace.service.FeedRecordService;
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
 * 饲料投喂记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/feed")
@Tag(name = "饲料投喂管理", description = "饲料投喂记录接口")
public class FeedRecordController {

    @Autowired
    private FeedRecordService feedRecordService;

    @PostMapping
    @Operation(summary = "创建饲料投喂记录", description = "创建新的饲料投喂记录")
    public ResponseEntity<Map<String, Object>> createFeedRecord(
            @Validated @RequestBody CreateFeedRecordRequest request) {
        try {
            String metaHash = feedRecordService.createFeedRecord(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("metaHash", metaHash);
            result.put("recordId", request.getRecordId());
            result.put("message", "饲料投喂记录创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建饲料投喂记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "查询养殖群饲料记录", description = "根据养殖群ID查询所有饲料投喂记录")
    public ResponseEntity<Map<String, Object>> listByGroupId(@PathVariable String groupId) {
        try {
            List<FeedRecordDTO> list = feedRecordService.listByGroupId(groupId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询饲料记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询饲料记录详情", description = "根据记录ID查询饲料投喂详情")
    public ResponseEntity<Map<String, Object>> getByRecordId(@PathVariable String recordId) {
        try {
            FeedRecordDTO dto = feedRecordService.getByRecordId(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询饲料记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/{recordId}/void")
    @Operation(summary = "作废饲料投喂记录", description = "将饲料投喂记录标记为已作废")
    public ResponseEntity<Map<String, Object>> voidRecord(@PathVariable String recordId) {
        try {
            feedRecordService.voidRecord(recordId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "记录已作废");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("作废饲料记录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
