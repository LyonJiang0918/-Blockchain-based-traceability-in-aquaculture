#!/usr/bin/env python3
"""
FISCO BCOS 智能合约部署脚本
通过 RPC API 直接部署合约，并先做连通性诊断
"""
import json
import time
import requests

RPC_URL = "http://127.0.0.1:20200"
CONTRACT_DIR = r"C:\Users\24835\Desktop\F\backend\src\main\resources\contracts\compiled"


def call_rpc(method, params=None):
    """发送 RPC 请求并打印诊断信息"""
    if params is None:
        params = []

    payload = {
        "jsonrpc": "2.0",
        "method": method,
        "params": params,
        "id": 1,
    }

    try:
        response = requests.post(RPC_URL, json=payload, timeout=30)
        print(f"  <- HTTP {response.status_code}")
        print(f"  <- Body: {response.text[:500]}")
        response.raise_for_status()
        return response.json()
    except Exception as e:
        print(f"RPC 调用失败: {e}")
        return None


def load_contract(name):
    """加载合约文件"""
    try:
        with open(f"{CONTRACT_DIR}\\{name}.abi", "r", encoding="utf-8") as f:
            abi = json.load(f)
        with open(f"{CONTRACT_DIR}\\{name}.bin", "r", encoding="utf-8") as f:
            bin_code = "0x" + f.read().strip()
        return abi, bin_code
    except Exception as e:
        print(f"加载合约失败 {name}: {e}")
        return None, None


def diagnose_rpc():
    print("\n========== RPC 诊断 ==========")
    print(f"RPC_URL = {RPC_URL}")

    for method, params in [
        ("eth_blockNumber", []),
        ("eth_accounts", []),
    ]:
        print(f"\n-> {method}")
        result = call_rpc(method, params)
        print(f"  result = {result}")

    print("========== 诊断结束 ==========")



def get_account():
    """获取默认账户"""
    result = call_rpc("eth_accounts")
    if result and "result" in result and result["result"]:
        return result["result"][0]
    return None


def deploy_contract(name, bin_code, from_address):
    """部署合约"""
    print(f"\n部署合约: {name}")
    print(f"  BIN: {bin_code[:50]}...")

    params = {
        "from": from_address,
        "data": bin_code,
        "gas": "0x1000000"
    }

    result = call_rpc("eth_sendTransaction", [params])

    if result and "result" in result:
        tx_hash = result["result"]
        print(f"  交易哈希: {tx_hash}")
        print("  等待确认...")
        for i in range(30):
            time.sleep(2)
            receipt = call_rpc("eth_getTransactionReceipt", [tx_hash])
            if receipt and "result" in receipt and receipt["result"]:
                receipt_data = receipt["result"]
                if receipt_data.get("contractAddress"):
                    address = receipt_data["contractAddress"]
                    print("  部署成功!")
                    print(f"  合约地址: {address}")
                    return address
                elif receipt_data.get("status") == "0x0":
                    print("  部署失败")
                    return None
            print(f"  等待中... ({i+1}/30)")

    if result and "error" in result:
        print(f"  部署失败: {result['error']}")

    return None


def main():
    print("=" * 50)
    print("  FISCO BCOS 智能合约部署器")
    print("=" * 50)

    diagnose_rpc()

    print("\n检查节点连接...")
    result = call_rpc("eth_blockNumber")
    if result and "result" in result:
        try:
            block_number = int(result["result"], 16)
            print(f"  节点正常! 当前区块: {block_number}")
        except Exception:
            print(f"  节点返回非标准区块号: {result['result']}")
    else:
        print("  无法连接到节点!")
        return

    print("\n获取部署账户...")
    account = get_account()
    if account:
        print(f"  账户: {account}")
    else:
        print("  无法获取账户")
        return

    contracts = [("BatchRegistry", "批次登记合约")]
    deployed = {}

    for name, desc in contracts:
        print("\n" + "-" * 50)
        print(f"【{desc}】")
        abi, bin_code = load_contract(name)
        if abi and bin_code:
            address = deploy_contract(name, bin_code, account)
            if address:
                deployed[name] = address

    print("\n" + "=" * 50)
    print("  部署结果")
    print("=" * 50)

    if deployed:
        print("\n部署成功的合约:")
        for name, address in deployed.items():
            print(f"  {name}: {address}")

        batch_registry = deployed.get("BatchRegistry")
        if batch_registry:
            print("\n请将以下配置填入 backend/src/main/resources/application-dev.yml:")
            print("-" * 50)
            print(f"  contract.batch-registry: {batch_registry}")
            try:
                with open("C:\\Users\\24835\\Desktop\\F\\batch-registry-address.txt", "w", encoding="utf-8") as f:
                    f.write(batch_registry)
                print("\nBatchRegistry 地址已写入: batch-registry-address.txt")
            except Exception as e:
                print(f"\n写入地址文件失败: {e}")
    else:
        print("\n没有合约部署成功")


if __name__ == "__main__":
    main()
