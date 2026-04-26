package com.trace.contract;

/**
 * TraceRegistry 合约包装类（手工骨架版）。
 *
 * 说明：
 * - 该类的命名、方法名与 TraceRegistry.sol 保持一一对应，方便后续替换成 sol2java 生成版本。
 * - 当前仅保留 ABI 常量与方法名映射，不包含完整的 SDK 编解码实现。
 */
public final class TraceRegistry {

    private TraceRegistry() {
    }

    public static final String CONTRACT_NAME = "TraceRegistry";

    public static final String FUNC_CREATE_BATCH = "createBatch";
    public static final String FUNC_UPDATE_BATCH_STATUS = "updateBatchStatus";
    public static final String FUNC_ADMIN_ROLLBACK = "adminRollback";
    public static final String FUNC_START_PROCESS = "startProcess";
    public static final String FUNC_COMPLETE_PROCESS = "completeProcess";
    public static final String FUNC_CREATE_FEED_RECORD = "createFeedRecord";
    public static final String FUNC_CREATE_VET_RECORD = "createVetRecord";
    public static final String FUNC_CREATE_INSPECTION_RECORD = "createInspectionRecord";
    public static final String FUNC_CREATE_TRANSFER_RECORD = "createTransferRecord";

    public static final String ABI = "[{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"batchId\",\"type\":\"string\"}],\"name\":\"BatchCreated\",\"type\":\"event\"}]";
}
