package com.trace.dao;

import com.trace.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    /** 根据养殖群ID查询所有动物 */
    List<Animal> findByBatchId(String batchId);

    /** 根据耳标号精确查询 */
    Optional<Animal> findByAnimalId(String animalId);

    /** 统计某群动物数量 */
    long countByBatchId(String batchId);

    /** 根据耳标号查询（支持模糊匹配） */
    List<Animal> findByAnimalIdContaining(String animalId);

    /** 根据养殖场ID查询所有动物 */
    List<Animal> findByFarmId(String farmId);
}
