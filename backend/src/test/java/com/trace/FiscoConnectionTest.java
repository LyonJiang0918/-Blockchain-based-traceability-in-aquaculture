package com.trace;

import com.trace.contract.BatchRegistry;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires a running FISCO BCOS node (default peer: 127.0.0.1:20200).")
class FiscoConnectionTest {

    @Autowired(required = false)
    private Client client;

    @Test
    void testBlockNumber() throws Exception {
        if (client == null) {
            throw new IllegalStateException("Client bean 未注入成功，请检查 FISCO BCOS 配置");
        }

        Object blockNumber = client.getBlockNumber();
        System.out.println("当前区块高: " + blockNumber);
    }

    @Test
    void testDeployBatchRegistryContract() throws Exception {
        if (client == null) {
            throw new IllegalStateException("Client bean 未注入成功，无法部署合约");
        }

        CryptoKeyPair keyPair = client.getCryptoSuite().getCryptoKeyPair();
        BatchRegistry contract = BatchRegistry.deploy(client, keyPair);
        System.out.println("🎉 合约部署成功！地址: " + contract.getContractAddress());
    }
}
