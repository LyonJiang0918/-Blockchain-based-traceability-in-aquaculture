package com.trace.dao;

import com.trace.entity.ByProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 农副产品数据访问层
 */
@Repository
public interface ByProductRepository extends JpaRepository<ByProduct, Long> {

    /**
     * 根据副产品ID查询
     */
    Optional<ByProduct> findByProductId(String productId);

    /**
     * 根据养殖群ID查询所有副产品
     */
    List<ByProduct> findByGroupId(String groupId);

    /**
     * 根据养殖群ID和状态查询副产品
     */
    List<ByProduct> findByGroupIdAndStatus(String groupId, Integer status);

    /**
     * 根据副产品类型查询
     */
    List<ByProduct> findByProductType(String productType);

    /**
     * 检查副产品ID是否存在
     */
    boolean existsByProductId(String productId);

    /**
     * 检查养殖群ID是否存在
     */
    boolean existsByGroupId(String groupId);
}
