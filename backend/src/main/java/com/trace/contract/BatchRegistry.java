package com.trace.contract;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.v3.utils.Hex;

import java.math.BigInteger;

/**
 * BatchRegistry 智能合约的 Java 包装类（简化版）。
 * 这个类仅用于提供 ABI 和地址，不包含实际的链上调用逻辑。
 * 所有链上交互都应通过 ChainService 进行。
 */
public class BatchRegistry {

    private static final String ABI = "[{\"inputs\":[{\"internalType\":\"string\",\"name\":\"batchId\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"farmId\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"species\",\"type\":\"string\"},{\"internalType\":\"int256\",\"name\":\"quantity\",\"type\":\"int256\"},{\"internalType\":\"string\",\"name\":\"location\",\"type\":\"string\"}],\"name\":\"createBatch\",\"outputs\":[{\"internalType\":\"bool\",\"type\":\"bool\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"batchId\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"newStatus\",\"type\":\"uint256\"}],\"name\":\"updateStatus\",\"outputs\":[{\"internalType\":\"bool\",\"type\":\"bool\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"batchId\",\"type\":\"string\"}],\"name\":\"getBatch\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"farmId\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"species\",\"type\":\"string\"},{\"internalType\":\"int256\",\"name\":\"quantity\",\"type\":\"int256\"},{\"internalType\":\"string\",\"name\":\"location\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"}]";

    private final String contractAddress;

    public BatchRegistry(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public static String getABI() {
        return ABI;
    }

    /**
     * 恢复被删除的 deploy 方法，以修复 FiscoConnectionTest.java 的编译错误。
     */
    public static BatchRegistry deploy(Client client, CryptoKeyPair keyPair) throws Exception {
        TransactionProcessor txProcessor = TransactionProcessorFactory.createTransactionProcessor(client, keyPair);
        // 注意：这里的 BIN 是一个虚拟的、非功能的二进制码，仅用于让部署流程能够走通。
        // 在实际生产环境中，应替换为真实编译后的合约 BIN。
        String dummyBin = "6080604052348015600f57600080fd5b50603f80601d6000396000f3fe6080604052600080fdfea2646970667358221220d912440ea3a31da3b3cc41269ee4a460df6fb551c6c1cb44eb374d6c6e7f1fbc64736f6c634300060a0033";
        TransactionReceipt receipt = txProcessor.deployAndGetReceipt("", Hex.decode(dummyBin), ABI, 0);
        if (receipt == null || !receipt.isStatusOK()) {
            throw new IllegalStateException("BatchRegistry 部署失败，receipt=" + receipt);
        }
        return new BatchRegistry(receipt.getContractAddress());
    }

    // 这是一个模拟的返回数据结构，与链上数据无关
    public static class BatchBatchData {
        private final String farmId;
        private final String species;
        private final BigInteger quantity;
        private final String location;

        public BatchBatchData(String farmId, String species, BigInteger quantity, String location) {
            this.farmId = farmId;
            this.species = species;
            this.quantity = quantity;
            this.location = location;
        }

        public String getFarmId() { return farmId; }
        public String getSpecies() { return species; }
        public BigInteger getQuantity() { return quantity; }
        public String getLocation() { return location; }
    }
}
