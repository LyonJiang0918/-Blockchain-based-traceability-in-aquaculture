package com.trace.contract;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * FISCO BCOS 智能合约部署器 - 使用 HttpsURLConnection
 */
public class ContractDeployerSSL {

    private static final String RPC_HOST = "127.0.0.1";
    private static final int RPC_PORT = 20200;

    private static void setupSSL() throws Exception {
        // 创建信任所有证书的 TrustManager
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        // 安装全局 SSL Socket Factory
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        // 禁用主机名验证
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        System.out.println("  SSL 配置完成（开发模式）");
    }

    private static String sendRpc(String method, Object[] params) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("params", params);
        request.put("id", 1);

        String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);

        URL url = new URL("https://" + RPC_HOST + ":" + RPC_PORT);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        conn.connect();

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        conn.disconnect();

        Map<String, Object> resp = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                response.toString(), Map.class);
        Object result = resp.get("result");
        return result != null ? result.toString() : null;
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          FISCO BCOS 智能合约部署器                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        String contractDir = "C:/Users/24835/Desktop/F/backend/src/main/resources/contracts/compiled";
        System.out.println("合约目录: " + contractDir);
        System.out.println();

        String[] contracts = {"BatchRegistry", "FeedRecord", "VetRecord", "InspectionRecord", "TransferRecord"};
        String[] namesCN = {"批次登记合约", "饲料记录合约", "兽医记录合约", "检验记录合约", "流转记录合约"};

        try {
            // 设置 SSL
            System.out.println("配置 SSL...");
            setupSSL();

            // 检查连接
            System.out.println("\n检查节点连接...");
            String blockNumber = sendRpc("eth_blockNumber", new Object[]{});
            if (blockNumber != null && !blockNumber.equals("null")) {
                System.out.println("  ✓ 节点已连接!");
            } else {
                System.out.println("  ✗ 无法连接到节点!");
                return;
            }

            // 获取账户
            System.out.println("\n获取账户...");
            String accountsJson = sendRpc("eth_accounts", new Object[]{});
            String account = "0x0000000000000000000000000000000000000000";
            if (accountsJson != null && !accountsJson.equals("null")) {
                // 解析 JSON 数组
                account = accountsJson.replace("[", "").replace("]", "").replace("\"", "").trim();
                if (account.contains(",")) {
                    account = account.split(",")[0].trim();
                }
            }
            System.out.println("  ✓ 账户: " + account);

            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("开始部署智能合约");
            System.out.println("══════════════════════════════════════════════════════════\n");

            Map<String, String> deployed = new LinkedHashMap<>();

            for (int i = 0; i < contracts.length; i++) {
                String name = contracts[i];
                String desc = namesCN[i];

                System.out.println("【" + desc + "】");
                System.out.println("  合约名: " + name);

                String binPath = contractDir + "/" + name + ".bin";
                if (!Files.exists(Paths.get(binPath))) {
                    System.out.println("  ✗ 文件不存在\n");
                    continue;
                }

                try {
                    String bin = new String(Files.readAllBytes(Paths.get(binPath)), "UTF-8").trim();

                    // 获取 nonce
                    String nonceJson = sendRpc("eth_getTransactionCount", new Object[]{account, "latest"});
                    String nonce = "0x1";
                    if (nonceJson != null) {
                        nonce = nonceJson.replace("\"", "");
                    }

                    // 获取 gas limit
                    String gasLimit = "0x1000000";

                    // 构建交易
                    Map<String, Object> tx = new LinkedHashMap<>();
                    tx.put("from", account);
                    tx.put("data", "0x" + bin);
                    tx.put("gas", gasLimit);
                    tx.put("nonce", nonce);

                    // 发送交易
                    System.out.println("  发送部署交易...");
                    String txResult = sendRpc("eth_sendTransaction", new Object[]{tx});

                    if (txResult != null && !txResult.equals("null")) {
                        String txHash = txResult.replace("\"", "");
                        System.out.println("  交易哈希: " + txHash);

                        // 等待确认
                        System.out.println("  等待确认...");
                        String receipt = null;
                        for (int j = 0; j < 30; j++) {
                            Thread.sleep(2000);
                            String receiptJson = sendRpc("eth_getTransactionReceipt", new Object[]{txHash});
                            if (receiptJson != null && receiptJson.contains("contractAddress")) {
                                receipt = receiptJson;
                                break;
                            }
                            System.out.println("    等待中... (" + (j + 1) + "/30)");
                        }

                        if (receipt != null) {
                            // 提取合约地址
                            int start = receipt.indexOf("contractAddress") + 20;
                            String temp = receipt.substring(start);
                            int end = temp.indexOf("\"");
                            String address = temp.substring(0, end);
                            deployed.put(name, address);
                            System.out.println("  ✓ 部署成功!");
                            System.out.println("    地址: " + address);
                        } else {
                            System.out.println("  ✗ 部署超时");
                        }
                    } else {
                        System.out.println("  ✗ 发送交易失败");
                    }
                } catch (Exception e) {
                    System.out.println("  ✗ 部署错误: " + e.getMessage());
                }
                System.out.println();
            }

            // 输出结果
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("部署完成");
            System.out.println("══════════════════════════════════════════════════════════");

            if (!deployed.isEmpty()) {
                System.out.println("\n已部署的合约:");
                for (Map.Entry<String, String> e : deployed.entrySet()) {
                    System.out.println("  " + e.getKey() + ": " + e.getValue());
                }

                System.out.println("\n请将以下配置填入 application.yml:");
                System.out.println("─".repeat(50));
                for (Map.Entry<String, String> e : deployed.entrySet()) {
                    System.out.println("  " + e.getKey().toLowerCase() + ": \"" + e.getValue() + "\"");
                }
            }

        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
