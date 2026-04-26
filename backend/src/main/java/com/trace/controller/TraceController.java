package com.trace.controller;

import com.trace.dto.BatchDTO;
import com.trace.service.BatchService;
import com.trace.service.ByProductService;
import com.trace.service.DeliveryRecordService;
import com.trace.service.FeedRecordService;
import com.trace.service.GrowthRecordService;
import com.trace.service.ProcessService;
import com.trace.service.TraceAssemblyService;
import com.trace.service.VetRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 溯源查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/trace")
@CrossOrigin(origins = "*")
@Tag(name = "溯源查询", description = "消费者溯源查询接口")
public class TraceController {

    @Autowired
    private BatchService batchService;

    @Autowired
    private ByProductService byProductService;

    @Autowired
    private FeedRecordService feedRecordService;

    @Autowired
    private VetRecordService vetRecordService;

    @Autowired
    private GrowthRecordService growthRecordService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private TraceAssemblyService traceAssemblyService;

    @Autowired
    private DeliveryRecordService deliveryRecordService;

    @GetMapping("/{groupId}")
    @Operation(summary = "查询完整溯源链", description = "根据养殖群ID查询完整的溯源信息（任意阶段可查，含参与方地址）")
    public ResponseEntity<Map<String, Object>> getTraceInfo(@PathVariable String groupId) {
        try {
            BatchDTO batch = batchService.getBatch(groupId);
            String gid = batch.getGroupId() != null ? batch.getGroupId() : groupId;

            Map<String, Object> traceInfo = new HashMap<>();
            traceInfo.put("batch", batch);

            // 参与方：养殖场 / 加工厂 / 销售商（地址来自用户档案 location，出栏后才有关联加工厂等）
            traceInfo.put("participants", traceAssemblyService.buildParticipants(gid, batch));

            // 送达关系（便于展示「已指定哪家加工厂」等）
            try {
                traceInfo.put("deliveryRecords", deliveryRecordService.getDeliveryRecordsByBatchId(gid));
            } catch (Exception e) {
                log.warn("查询送达记录失败: {}", gid, e);
                traceInfo.put("deliveryRecords", java.util.Collections.emptyList());
            }

            traceInfo.put("feedRecords", safeList(() -> feedRecordService.listByGroupId(gid)));
            traceInfo.put("vetRecords", safeList(() -> vetRecordService.listByGroupId(gid)));
            traceInfo.put("growthRecords", safeList(() -> growthRecordService.listByGroupId(gid)));
            traceInfo.put("byProducts", safeList(() -> byProductService.listByGroupId(gid)));
            traceInfo.put("processRecords", safeList(() -> processService.listByBatchId(gid)));

            traceInfo.put("inspections", java.util.Collections.emptyList());
            traceInfo.put("transfers", java.util.Collections.emptyList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", traceInfo);
            result.put("message", "查询成功");

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("查询溯源信息失败: {}", groupId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage() != null ? e.getMessage() : "查询失败");
            return ResponseEntity.badRequest().body(result);
        }
    }

    private static <T> java.util.List<T> safeList(java.util.concurrent.Callable<java.util.List<T>> call) {
        try {
            java.util.List<T> list = call.call();
            return list != null ? list : java.util.Collections.emptyList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}


