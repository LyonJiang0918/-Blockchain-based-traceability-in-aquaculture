package com.trace.dao;

import com.trace.entity.ProcessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessRecordRepository extends JpaRepository<ProcessRecord, Long> {

    /** 根据养殖群ID查询加工记录 */
    List<ProcessRecord> findByBatchId(String batchId);

    /** 根据记录ID精确查询 */
    Optional<ProcessRecord> findByRecordId(String recordId);

    /** 根据状态查询 */
    List<ProcessRecord> findByStatus(Integer status);

    /** 根据加工厂ID查询 */
    List<ProcessRecord> findByProcessFactoryId(String processFactoryId);
}
