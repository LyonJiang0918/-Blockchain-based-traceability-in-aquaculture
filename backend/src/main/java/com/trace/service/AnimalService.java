package com.trace.service;

import com.trace.dao.AnimalRepository;
import com.trace.dao.BatchMetaRepository;
import com.trace.dto.request.CreateAnimalRequest;
import com.trace.entity.Animal;
import com.trace.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 单体动物服务
 */
@Slf4j
@Service
public class AnimalService {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @Autowired
    private RolePermissionService rolePermissionService;

    private static final DateTimeFormatter ID_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 登记单体动物（自动生成耳标号）
     * @param request 登记请求
     * @param userRole 当前用户角色
     * @param userFarmId 当前用户所属养殖场ID
     */
    public String registerAnimal(CreateAnimalRequest request, String userRole, String userFarmId) {
        // 生成耳标号：ANIMAL + 年月日时分秒 + 6位随机码
        String animalId = generateAnimalId();
        request.setAnimalId(animalId);

        // 权限校验：管理员可操作所有批次；养殖场只能操作自己farmId的批次
        String batchFarmId = request.getFarmId();
        if (!rolePermissionService.canAddRecord(userRole, batchFarmId, userFarmId)) {
            throw new RuntimeException("无权限登记动物，只有管理员或所属养殖场可以操作");
        }

        // 保存动物记录
        Animal animal = new Animal();
        animal.setAnimalId(animalId);
        animal.setBatchId(request.getBatchId());
        animal.setSpeciesCategory(request.getSpeciesCategory());
        animal.setSpecies(request.getSpecies());
        animal.setFarmId(request.getFarmId());
        animal.setStatus(0);
        animal.setBirthTime(LocalDateTime.now());
        animal.setRemark(request.getRemark());

        // 计算哈希
        String hashInput = animalId + "\n" + request.getBatchId() + "\n" +
                (request.getFarmId() != null ? request.getFarmId() : "") + "\n" + animal.getStatus();
        animal.setIndividualHash(HashUtil.sha256(hashInput));

        animalRepository.save(animal);
        log.info("已登记单体动物，animalId={}, batchId={}", animalId, request.getBatchId());

        // 更新养殖群的动物计数
        updateBatchAnimalCount(request.getBatchId());

        return animalId;
    }

    /**
     * 批量登记动物
     */
    public List<String> registerAnimalsBatch(List<CreateAnimalRequest> requests,
                                             String userRole, String userFarmId) {
        List<String> animalIds = new ArrayList<>();
        String batchId = null;
        String farmId = null;
        String speciesCategory = null;
        String species = null;

        // 从第一条记录获取公共信息
        if (requests != null && !requests.isEmpty()) {
            batchId = requests.get(0).getBatchId();
            farmId = requests.get(0).getFarmId();
            speciesCategory = requests.get(0).getSpeciesCategory();
            species = requests.get(0).getSpecies();
        }

        if (batchId == null || farmId == null) {
            throw new RuntimeException("批量登记需要提供养殖群ID和养殖场ID");
        }

        // 权限校验
        if (!rolePermissionService.canAddRecord(userRole, farmId, userFarmId)) {
            throw new RuntimeException("无权限登记动物，只有管理员或所属养殖场可以操作");
        }

        for (CreateAnimalRequest request : requests) {
            String animalId = generateAnimalId();
            request.setAnimalId(animalId);
            request.setBatchId(batchId);
            request.setFarmId(farmId);
            request.setSpeciesCategory(speciesCategory);
            request.setSpecies(species);

            Animal animal = new Animal();
            animal.setAnimalId(animalId);
            animal.setBatchId(batchId);
            animal.setSpeciesCategory(speciesCategory);
            animal.setSpecies(species);
            animal.setFarmId(farmId);
            animal.setStatus(0);
            animal.setBirthTime(LocalDateTime.now());

            String hashInput = animalId + "\n" + batchId + "\n" + farmId + "\n" + animal.getStatus();
            animal.setIndividualHash(HashUtil.sha256(hashInput));

            animalRepository.save(animal);
            animalIds.add(animalId);
            log.info("已批量登记动物，animalId={}, batchId={}", animalId, batchId);
        }

        // 更新养殖群的动物计数
        updateBatchAnimalCount(batchId);

        return animalIds;
    }

    /**
     * 查询某养殖群的所有动物
     */
    public List<Animal> listByBatchId(String batchId) {
        return animalRepository.findByBatchId(batchId);
    }

    /**
     * 按耳标号查询动物
     */
    public Object getByAnimalId(String animalId) {
        Optional<Animal> opt = animalRepository.findByAnimalId(animalId);
        if (!opt.isPresent()) {
            throw new RuntimeException("动物不存在: " + animalId);
        }
        return opt.get();
    }

    /**
     * 批量更新动物状态（供 ProcessService 调用）
     */
    public void updateAnimalStatus(List<String> animalIds, Integer newStatus) {
        for (String animalId : animalIds) {
            Optional<Animal> opt = animalRepository.findByAnimalId(animalId);
            if (opt.isPresent()) {
                Animal animal = opt.get();
                animal.setStatus(newStatus);
                animalRepository.save(animal);
                log.info("已更新动物状态，animalId={}, status={}", animalId, newStatus);
            }
        }
    }

    /**
     * 生成唯一耳标号
     */
    private String generateAnimalId() {
        String timestamp = LocalDateTime.now().format(ID_DATE_FORMATTER);
        String random = String.format("%06d", new Random().nextInt(999999));
        return "ANIMAL" + timestamp + random;
    }

    /**
     * 更新养殖群的动物计数
     */
    private void updateBatchAnimalCount(String batchId) {
        batchMetaRepository.findByBatchId(batchId).ifPresent(meta -> {
            long count = animalRepository.countByBatchId(batchId);
            meta.setAnimalCount((int) count);
            batchMetaRepository.save(meta);
            log.info("已更新养殖群 {} 的动物计数: {}", batchId, count);
        });
    }
}
