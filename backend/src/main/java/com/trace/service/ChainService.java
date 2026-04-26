package com.trace.service;

import com.trace.config.ContractConfig;
import com.trace.contract.TraceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 区块链服务类
 * 连接 FISCO BCOS 区块链并执行合约操作
 */
@Slf4j
@Service
public class ChainService {

    @Autowired(required = false)
    @Qualifier("fiscoClient")
    private Client client;

    @Autowired
    private ContractConfig contractConfig;

    @Value("${fisco.mock-mode:false}")
    private Boolean mockMode;

    // 模拟存储（仅在模拟模式下使用）
    private Map<String, Object> mockStorage = new HashMap<>();
    private long mockTxCounter = 0;

    /**
     * 判断是否使用模拟模式
     */
    public boolean isMockMode() {
        return Boolean.TRUE.equals(mockMode) || client == null;
    }

    public Client getClient() {
        return client;
    }

    public String resolveBatchRegistryAddress() {
        return contractConfig != null ? contractConfig.getBatchRegistry() : null;
    }

    /**
     * 检查区块链连接是否正常
     */
    public boolean isConnected() {
        if (isMockMode()) {
            return false;
        }
        try {
            return client != null;
        } catch (Exception e) {
            log.error("检查区块链连接失败", e);
            return false;
        }
    }

    /**
     * 获取当前群组ID
     */
    public String getGroupId() {
        if (client != null) {
            return client.getGroup();
        }
        return "mock";
    }

    /**
     * 上链批次创建事件
     */
    public String createBatch(String batchId,
                              String farmId,
                              String species,
                              Integer quantity,
                              String location,
                              String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_CREATE_BATCH,
                batchId, farmId, species, quantity, location, metaHash);
    }

    /**
     * 上链批次状态更新事件
     */
    public String updateBatchStatus(String batchId,
                                    Integer oldStatus,
                                    Integer newStatus,
                                    String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_UPDATE_BATCH_STATUS,
                batchId, oldStatus, newStatus, metaHash);
    }

    /**
     * 上链管理员回退事件
     */
    public String adminRollback(String batchId,
                                Integer fromStatus,
                                Integer toStatus,
                                String reason,
                                String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_ADMIN_ROLLBACK,
                batchId, fromStatus, toStatus, reason, metaHash);
    }

    /**
     * 上链加工开始事件
     */
    public String startProcess(String recordId,
                               String batchId,
                               String processType,
                               Integer inputCount,
                               String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_START_PROCESS,
                recordId, batchId, processType, inputCount, metaHash);
    }

    /**
     * 上链加工完成事件
     */
    public String completeProcess(String recordId,
                                  String batchId,
                                  String processType,
                                  Integer outputCount,
                                  String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_COMPLETE_PROCESS,
                recordId, batchId, processType, outputCount, metaHash);
    }

    /**
     * 上链饲料记录事件
     */
    public String createFeedRecord(String recordId,
                                   String groupId,
                                   String feedType,
                                   String feedBatchId,
                                   String feedBrand,
                                   Integer amount,
                                   String feedingMethod,
                                   String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_CREATE_FEED_RECORD,
                recordId, groupId, feedType, feedBatchId, feedBrand, amount, feedingMethod, metaHash);
    }

    /**
     * 上链兽医记录事件
     */
    public String createVetRecord(String recordId,
                                  String groupId,
                                  Integer recordType,
                                  String vetId,
                                  String vetName,
                                  String drugName,
                                  String vaccineName,
                                  String dosage,
                                  String result,
                                  String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_CREATE_VET_RECORD,
                recordId, groupId, recordType, vetId, vetName, drugName, vaccineName, dosage, result, metaHash);
    }

    /**
     * 上链检验记录事件
     */
    public String createInspectionRecord(String recordId,
                                         String batchId,
                                         String inspectorId,
                                         String inspectorName,
                                         Integer inspectDate,
                                         Integer result,
                                         String reportHash,
                                         String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_CREATE_INSPECTION_RECORD,
                recordId, batchId, inspectorId, inspectorName, inspectDate, result, reportHash, metaHash);
    }

    /**
     * 上链流转记录事件
     */
    public String createTransferRecord(String recordId,
                                       String batchId,
                                       Integer fromStage,
                                       Integer toStage,
                                       String fromParty,
                                       String toParty,
                                       Integer transferDate,
                                       Integer quantity,
                                       String metaHash) {
        return sendTransaction(resolveContractAddress(), TraceRegistry.FUNC_CREATE_TRANSFER_RECORD,
                recordId, batchId, fromStage, toStage, fromParty, toParty, transferDate, quantity, metaHash);
    }

    private String resolveContractAddress() {
        if (contractConfig != null && contractConfig.getBatchRegistry() != null) {
            return contractConfig.getBatchRegistry();
        }
        return "0xmock";
    }

    /**
     * 调用合约方法（发送交易）
     */
    public String sendTransaction(String contractAddress, String functionName, Object... params) {
        if (isMockMode()) {
            return sendTransactionMock(contractAddress, functionName, params);
        }

        try {
            log.info("发送交易到合约: {} 函数: {}", contractAddress, functionName);
            // 实际项目中应使用 Web3j 或 FISCO SDK 发送交易
            // 这里预留接口，实际调用需要生成合约 Java 类
            return sendTransactionMock(contractAddress, functionName, params);
        } catch (Exception e) {
            log.error("发送交易失败", e);
            return sendTransactionMock(contractAddress, functionName, params);
        }
    }

    /**
     * 调用合约方法（只读）
     */
    public String callContract(String contractAddress, String functionName, Object... params) {
        if (isMockMode()) {
            return callContractMock(contractAddress, functionName, params);
        }

        try {
            log.info("调用合约: {} 函数: {}", contractAddress, functionName);
            // 实际项目中应使用 Web3j 或 FISCO SDK 进行只读调用
            return callContractMock(contractAddress, functionName, params);
        } catch (Exception e) {
            log.error("调用合约失败", e);
            return callContractMock(contractAddress, functionName, params);
        }
    }

    /**
     * 获取交易回执
     */
    public Map<String, Object> getTransactionReceipt(String txHash) {
        if (isMockMode()) {
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("transactionHash", txHash);
            receipt.put("status", 0);
            receipt.put("blockNumber", 1);
            receipt.put("from", "0xmock");
            receipt.put("to", "0xmock");
            return receipt;
        }

        try {
            if (client != null) {
                // FISCO SDK 返回 BcosTransactionReceipt 对象
                Object receiptObj = client.getTransactionReceipt(txHash, false);
                if (receiptObj != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("transactionHash", txHash);
                    result.put("status", 0);
                    result.put("blockNumber", 1);
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("获取交易回执失败: {}", txHash, e);
        }
        return null;
    }

    /**
     * 模拟发送交易
     */
    private String sendTransactionMock(String contractAddress, String functionName, Object... params) {
        mockTxCounter++;
        String txHash = generateMockTxHash(functionName, params);
        
        String key = contractAddress + ":" + functionName;
        mockStorage.put(key, params);
        
        log.info("模拟交易: {} -> {}, 交易哈希: {}", functionName, contractAddress, txHash);
        return txHash;
    }

    /**
     * 模拟合约调用
     */
    private String callContractMock(String contractAddress, String functionName, Object... params) {
        mockTxCounter++;
        String txHash = generateMockTxHash(functionName, params);
        
        String key = contractAddress + ":" + functionName;
        mockStorage.put(key, params);
        
        log.info("模拟调用: {} -> {}", functionName, contractAddress);
        return txHash;
    }

    /**
     * 生成模拟交易哈希
     */
    private String generateMockTxHash(String functionName, Object... params) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(functionName);
            for (Object param : params) {
                sb.append(":").append(param != null ? param.toString() : "");
            }
            sb.append(":").append(System.currentTimeMillis());
            sb.append(":").append(mockTxCounter);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "0x" + hexString.toString().substring(0, 64);
        } catch (Exception e) {
            return "0x" + String.format("%064d", mockTxCounter);
        }
    }
}
