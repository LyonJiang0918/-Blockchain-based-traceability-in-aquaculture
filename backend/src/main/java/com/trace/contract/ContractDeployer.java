package com.trace.contract;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * FISCO BCOS 智能合约部署器
 * 使用 Java HttpClient 直接调用 RPC
 */
public class ContractDeployer {

    private static final String RPC_URL = "http://127.0.0.1:20200";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String[] CONTRACTS = {
        "BatchRegistry",
        "FeedRecord",
        "VetRecord",
        "InspectionRecord",
        "TransferRecord"
    };

    private static final String[] CONTRACT_NAMES_CN = {
        "批次登记合约",
        "饲料记录合约",
        "兽医记录合约",
        "检验记录合约",
        "流转记录合约"
    };

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          FISCO BCOS 智能合约部署器                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        String contractDir = findContractDir();
        System.out.println("合约目录: " + contractDir);
        System.out.println();

        // 检查连接
        System.out.println("检查节点连接...");
        String blockNumber = callRpc("eth_blockNumber", new Object[]{});
        if (blockNumber != null) {
            System.out.println("  ✓ 节点已连接，区块: " + blockNumber);
        } else {
            System.out.println("  ✗ 无法连接到节点!");
            System.out.println("请确保 FISCO BCOS 节点正在运行且 RPC 已启用");
            return;
        }

        // 获取账户
        System.out.println();
        System.out.println("获取部署账户...");
        String[] accounts = getAccounts();
        if (accounts == null || accounts.length == 0) {
            System.out.println("  ✗ 没有可用账户");
            return;
        }
        String account = accounts[0];
        System.out.println("  ✓ 账户: " + account);

        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("开始部署智能合约");
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println();

        Map<String, String> deployed = new LinkedHashMap<>();

        for (int i = 0; i < CONTRACTS.length; i++) {
            String name = CONTRACTS[i];
            String desc = CONTRACT_NAMES_CN[i];

            System.out.println("【" + desc + "】");
            System.out.println("  合约名: " + name);

            String abiPath = contractDir + "/" + name + ".abi";
            String binPath = contractDir + "/" + name + ".bin";

            if (!Files.exists(Paths.get(abiPath)) || !Files.exists(Paths.get(binPath))) {
                System.out.println("  ✗ 合约文件不存在");
                System.out.println();
                continue;
            }

            try {
                String bin = new String(Files.readAllBytes(Paths.get(binPath)), StandardCharsets.UTF_8).trim();
                System.out.println("  正在部署...");

                String address = deploy(account, "0x" + bin);

                if (address != null) {
                    deployed.put(name, address);
                    System.out.println("  ✓ 部署成功!");
                    System.out.println("    地址: " + address);
                } else {
                    System.out.println("  ✗ 部署失败");
                }
            } catch (Exception e) {
                System.out.println("  ✗ 部署出错: " + e.getMessage());
            }
            System.out.println();
        }

        // 输出结果
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("部署完成");
        System.out.println("══════════════════════════════════════════════════════════");

        if (!deployed.isEmpty()) {
            System.out.println();
            System.out.println("已部署的合约:");
            for (Map.Entry<String, String> entry : deployed.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println();
            System.out.println("请将以下配置填入 application.yml:");
            System.out.println("─".repeat(50));
            for (Map.Entry<String, String> entry : deployed.entrySet()) {
                String key = entry.getKey().toLowerCase();
                System.out.println("  " + key + ": \"" + entry.getValue() + "\"");
            }

            // 保存到文件
            saveConfig(deployed);
        } else {
            System.out.println();
            System.out.println("没有合约部署成功");
        }
    }

    private static String callRpc(String method, Object[] params) {
        try {
            Map<String, Object> rpcRequest = new LinkedHashMap<>();
            rpcRequest.put("jsonrpc", "2.0");
            rpcRequest.put("method", method);
            rpcRequest.put("params", params);
            rpcRequest.put("id", 1);

            String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rpcRequest);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(RPC_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("---- RPC Debug: Request to " + RPC_URL + " method=" + method);
            System.out.println("Request body: " + requestBody);
            System.out.println("HTTP status: " + response.statusCode());
            System.out.println("HTTP body: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> resp = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        response.body(), Map.class);
                if (resp.containsKey("result")) {
                    return String.valueOf(resp.get("result"));
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("RPC call exception: " + e.getMessage());
            e.printStackTrace(System.out);
            return null;
        }
    }

    private static String[] getAccounts() {
        String result = callRpc("eth_accounts", new Object[]{});
        if (result != null) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        result, String[].class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String getTransactionCount(String address) {
        String result = callRpc("eth_getTransactionCount", new Object[]{address, "latest"});
        if (result != null) {
            return result.replace("\"", "");
        }
        return "0x0";
    }

    private static String getBlockByNumber() {
        String result = callRpc("eth_getBlockByNumber", new Object[]{"latest", false});
        if (result != null) {
            try {
                Map<String, Object> block = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        result, Map.class);
                if (block.containsKey("result") && block.get("result") != null) {
                    Map<String, Object> blockData = (Map<String, Object>) block.get("result");
                    return String.valueOf(blockData.get("gasLimit"));
                }
            } catch (Exception e) {
            }
        }
        return "0x1000000";
    }

    private static String deploy(String from, String bin) {
        try {
            // 获取 nonce
            String nonce = getTransactionCount(from);
            String gasLimit = getBlockByNumber();

            // 构建交易数据
            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("from", from);
            tx.put("data", bin);
            tx.put("gas", gasLimit);
            tx.put("nonce", nonce);

            // 发送交易
            String txHash = callRpc("eth_sendTransaction", new Object[]{tx});

            if (txHash == null || txHash.equals("null")) {
                return null;
            }

            txHash = txHash.replace("\"", "");
            System.out.println("    交易哈希: " + txHash);

            // 等待确认
            System.out.println("    等待确认...");
            String receipt = waitForReceipt(txHash, 60);

            if (receipt != null) {
                Map<String, Object> receiptData = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(receipt, Map.class);
                if (receiptData.containsKey("result") && receiptData.get("result") != null) {
                    Map<String, Object> result = (Map<String, Object>) receiptData.get("result");
                    String status = String.valueOf(result.get("status"));
                    if (status.equals("0x1")) {
                        return String.valueOf(result.get("contractAddress"));
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("    部署错误: " + e.getMessage());
            return null;
        }
    }

    private static String waitForReceipt(String txHash, int timeout) {
        for (int i = 0; i < timeout; i++) {
            try {
                String result = callRpc("eth_getTransactionReceipt", new Object[]{txHash});
                if (result != null && !result.equals("null")) {
                    Map<String, Object> receipt = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(result, Map.class);
                    if (receipt.containsKey("result") && receipt.get("result") != null) {
                        return result;
                    }
                }
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            System.out.println("    等待中... (" + (i + 1) + "/" + timeout + ")");
        }
        return null;
    }

    private static String findContractDir() {
        String[] paths = {
            "C:/Users/24835/Desktop/F/backend/src/main/resources/contracts/compiled",
            "src/main/resources/contracts/compiled"
        };
        for (String path : paths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }
        return "src/main/resources/contracts/compiled";
    }

    private static void saveConfig(Map<String, String> deployed) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# 智能合约部署地址\n");
            sb.append("contract:\n");
            for (Map.Entry<String, String> entry : deployed.entrySet()) {
                String key = entry.getKey().toLowerCase();
                sb.append("  ").append(key).append(": \"").append(entry.getValue()).append("\"\n");
            }
            Files.write(Paths.get("C:/Users/24835/Desktop/F/deployed-addresses.txt"),
                       sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println();
            System.out.println("配置已保存到: deployed-addresses.txt");
        } catch (IOException e) {
            System.out.println("保存配置失败: " + e.getMessage());
        }
    }
}
