package com.trace.service;

import com.trace.dao.BatchMetaRepository;
import com.trace.dao.ByProductRepository;
import com.trace.dto.ByProductDTO;
import com.trace.dto.request.CreateByProductRequest;
import com.trace.entity.ByProduct;
import com.trace.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 农副产品服务
 */
@Slf4j
@Service
public class ByProductService {

    @Autowired
    private ByProductRepository byProductRepository;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    /**
     * 创建农副产品
     */
    public String createByProduct(CreateByProductRequest request) {
        try {
            // 验证养殖群是否存在
            if (!batchMetaRepository.existsByBatchId(request.getGroupId())) {
                throw new RuntimeException("养殖群不存在: " + request.getGroupId());
            }

            // 检查副产品ID是否已存在
            if (byProductRepository.existsByProductId(request.getProductId())) {
                throw new RuntimeException("副产品ID已存在: " + request.getProductId());
            }

            // 构建副产品信息用于计算哈希
            String productInfo = buildProductInfo(request);
            String metaHash = HashUtil.sha256(request.getProductId() + "\n" + productInfo);

            // 创建副产品实体
            ByProduct byProduct = new ByProduct();
            byProduct.setProductId(request.getProductId());
            byProduct.setGroupId(request.getGroupId());
            byProduct.setProductType(request.getProductType());
            byProduct.setProductName(request.getProductName());
            byProduct.setQuantity(request.getQuantity());
            byProduct.setUnit(request.getUnit());
            byProduct.setProductionBatch(request.getProductionBatch());
            byProduct.setQualityGrade(request.getQualityGrade());
            byProduct.setStorageMethod(request.getStorageMethod());
            byProduct.setDescription(request.getDescription());
            byProduct.setMetaHash(metaHash);
            byProduct.setStatus(0);

            // 设置生产日期
            if (request.getProductionDate() != null) {
                byProduct.setProductionDate(
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(request.getProductionDate()),
                        ZoneId.systemDefault()
                    )
                );
            } else {
                byProduct.setProductionDate(LocalDateTime.now());
            }

            byProductRepository.save(byProduct);
            log.info("创建农副产品成功，productId={}, groupId={}", request.getProductId(), request.getGroupId());

            return metaHash;

        } catch (Exception e) {
            log.error("创建农副产品失败", e);
            throw new RuntimeException("创建农副产品失败: " + e.getMessage());
        }
    }

    /**
     * 查询养殖群的所有副产品
     */
    public List<ByProductDTO> listByGroupId(String groupId) {
        try {
            List<ByProduct> products = byProductRepository.findByGroupId(groupId);
            return products.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询副产品失败", e);
            throw new RuntimeException("查询副产品失败: " + e.getMessage());
        }
    }

    /**
     * 查询副产品详情
     */
    public ByProductDTO getByProduct(String productId) {
        try {
            Optional<ByProduct> opt = byProductRepository.findByProductId(productId);
            if (!opt.isPresent()) {
                throw new RuntimeException("副产品不存在: " + productId);
            }
            return convertToDTO(opt.get());
        } catch (Exception e) {
            log.error("查询副产品失败", e);
            throw new RuntimeException("查询副产品失败: " + e.getMessage());
        }
    }

    /**
     * 更新副产品状态
     */
    public void updateStatus(String productId, Integer status) {
        try {
            Optional<ByProduct> opt = byProductRepository.findByProductId(productId);
            if (!opt.isPresent()) {
                throw new RuntimeException("副产品不存在: " + productId);
            }
            ByProduct product = opt.get();
            product.setStatus(status);
            byProductRepository.save(product);
            log.info("更新副产品状态成功，productId={}, status={}", productId, status);
        } catch (Exception e) {
            log.error("更新副产品状态失败", e);
            throw new RuntimeException("更新副产品状态失败: " + e.getMessage());
        }
    }

    /**
     * 构建副产品信息字符串（用于计算哈希）
     */
    private String buildProductInfo(CreateByProductRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"productId\":\"").append(request.getProductId()).append("\",");
        sb.append("\"groupId\":\"").append(request.getGroupId()).append("\",");
        sb.append("\"productType\":\"").append(request.getProductType()).append("\",");
        sb.append("\"productName\":\"").append(request.getProductName()).append("\",");
        sb.append("\"quantity\":").append(request.getQuantity()).append(",");
        sb.append("\"unit\":\"").append(request.getUnit()).append("\"");
        if (request.getQualityGrade() != null) {
            sb.append(",\"qualityGrade\":\"").append(request.getQualityGrade()).append("\"");
        }
        if (request.getStorageMethod() != null) {
            sb.append(",\"storageMethod\":\"").append(request.getStorageMethod()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转换为DTO
     */
    private ByProductDTO convertToDTO(ByProduct product) {
        ByProductDTO dto = new ByProductDTO();
        dto.setId(product.getId());
        dto.setProductId(product.getProductId());
        dto.setGroupId(product.getGroupId());
        dto.setProductType(product.getProductType());
        dto.setProductTypeText(product.getProductTypeText());
        dto.setProductName(product.getProductName());
        dto.setQuantity(product.getQuantity());
        dto.setUnit(product.getUnit());
        dto.setProductionBatch(product.getProductionBatch());
        dto.setQualityGrade(product.getQualityGrade());
        dto.setStorageMethod(product.getStorageMethod());
        dto.setDescription(product.getDescription());
        dto.setMetaHash(product.getMetaHash());
        dto.setStatus(product.getStatus());
        dto.setStatusText(product.getStatusText());
        if (product.getProductionDate() != null) {
            dto.setProductionDate(
                product.getProductionDate().atZone(ZoneId.systemDefault()).toEpochSecond()
            );
        }
        if (product.getCreatedAt() != null) {
            dto.setCreatedAt(
                product.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
            );
        }
        return dto;
    }
}
