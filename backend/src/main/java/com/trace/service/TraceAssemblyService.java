package com.trace.service;

import com.trace.dao.UserRepository;
import com.trace.dto.BatchDTO;
import com.trace.dto.trace.TraceParticipantDTO;
import com.trace.entity.DeliveryRecord;
import com.trace.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 组装消费者溯源页所需的参与方、地址等信息（任意流程阶段均可查询）
 */
@Slf4j
@Service
public class TraceAssemblyService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryRecordService deliveryRecordService;

    /**
     * 组装参与方：养殖场 / 加工厂 / 销售商（无则返回 null，前端展示「未关联」）
     * 地址取自 sys_user.location（项目内已统一为各角色经营地址）
     */
    public Map<String, TraceParticipantDTO> buildParticipants(String batchId, BatchDTO batch) {
        Map<String, TraceParticipantDTO> out = new HashMap<>();
        out.put("farm", resolveFarm(batch));
        out.put("process", resolveProcess(batchId));
        out.put("sales", resolveSales(batchId));
        return out;
    }

    private TraceParticipantDTO resolveFarm(BatchDTO batch) {
        if (batch == null) {
            return null;
        }
        String fid = batch.getFarmId();
        if (fid == null || fid.trim().isEmpty()) {
            return null;
        }
        fid = fid.trim();
        List<User> byFarm = userRepository.findByFarmId(fid);
        if (!byFarm.isEmpty()) {
            return TraceParticipantDTO.fromUser(pickFarmUser(byFarm), "养殖场");
        }
        Optional<User> byName = userRepository.findByUsername(fid);
        if (byName.isPresent() && byName.get().getRole() == User.Role.FARM) {
            return TraceParticipantDTO.fromUser(byName.get(), "养殖场");
        }
        Optional<User> byUsernameLower = userRepository.findByUsername(fid.toLowerCase());
        if (byUsernameLower.isPresent() && byUsernameLower.get().getRole() == User.Role.FARM) {
            return TraceParticipantDTO.fromUser(byUsernameLower.get(), "养殖场");
        }
        Optional<User> byUsernameUpper = userRepository.findByUsername(fid.toUpperCase());
        if (byUsernameUpper.isPresent() && byUsernameUpper.get().getRole() == User.Role.FARM) {
            return TraceParticipantDTO.fromUser(byUsernameUpper.get(), "养殖场");
        }
        Optional<User> byUsernameExact = userRepository.findByUsername(fid);
        return byUsernameExact.map(u -> TraceParticipantDTO.fromUser(u, "养殖场")).orElse(null);
    }

    private User pickFarmUser(List<User> users) {
        return users.stream()
                .filter(u -> u.getRole() == User.Role.FARM)
                .findFirst()
                .orElse(users.get(0));
    }

    private TraceParticipantDTO resolveProcess(String batchId) {
        Optional<DeliveryRecord> dr = deliveryRecordService.getDeliveryRecordByBatchIdAndStage(
                batchId, DeliveryRecordService.STAGE_TO_PROCESS);
        if (!dr.isPresent()) {
            return null;
        }
        String toId = dr.get().getToId();
        if (toId == null || toId.isEmpty()) {
            return null;
        }
        return userRepository.findByUsername(toId)
                .filter(u -> u.getRole() == User.Role.PROCESS)
                .map(u -> TraceParticipantDTO.fromUser(u, "加工厂"))
                .orElseGet(() -> userRepository.findByUsername(toId)
                        .map(u -> TraceParticipantDTO.fromUser(u, "加工厂"))
                        .orElse(null));
    }

    private TraceParticipantDTO resolveSales(String batchId) {
        Optional<DeliveryRecord> dr = deliveryRecordService.getDeliveryRecordByBatchIdAndStage(
                batchId, DeliveryRecordService.STAGE_TO_SALES);
        if (!dr.isPresent()) {
            return null;
        }
        String toId = dr.get().getToId();
        if (toId == null || toId.isEmpty()) {
            return null;
        }
        return userRepository.findByUsername(toId)
                .filter(u -> u.getRole() == User.Role.SALES)
                .map(u -> TraceParticipantDTO.fromUser(u, "销售商"))
                .orElseGet(() -> userRepository.findByUsername(toId)
                        .map(u -> TraceParticipantDTO.fromUser(u, "销售商"))
                        .orElse(null));
    }
}
