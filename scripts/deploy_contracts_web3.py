#!/usr/bin/env python3
"""
FISCO BCOS 智能合约部署脚本
通过 Web3.py 库连接 FISCO BCOS 并部署合约
"""
import json
import time
import sys
from web3 import Web3

# FISCO BCOS RPC 地址（使用 HTTP）
RPC_URL = "http://127.0.0.1:20200"

# 合约文件目录
CONTRACT_DIR = "/mnt/c/Users/24835/Desktop/F/backend/src/main/resources/contracts/compiled"

def load_contract(name):
    """加载合约文件"""
    try:
        with open(f"{CONTRACT_DIR}/{name}.abi", "r") as f:
            abi = json.load(f)
        with open(f"{CONTRACT_DIR}/{name}.bin", "r") as f:
            bin_code = "0x" + f.read().strip()
        return abi, bin_code
    except Exception as e:
        print(f"  加载失败: {e}")
        return None, None

def check_connection():
    """检查连接"""
    try:
        w3 = Web3(Web3.HTTPProvider(RPC_URL))
        if w3.isConnected():
            block = w3.eth.block_number
            print(f"  ✓ 节点已连接，当前区块: {block}")
            return w3
        else:
            print("  ✗ 连接失败")
            return None
    except Exception as e:
        print(f"  ✗ 连接错误: {e}")
        return None

def deploy_contract(w3, name, abi, bin_code):
    """部署合约"""
    try:
        print(f"  部署中...")

        # 获取账户
        accounts = w3.eth.accounts
        if not accounts:
            print("  ✗ 没有可用账户")
            return None

        account = accounts[0]
        print(f"  使用账户: {account}")

        # 构建交易
        nonce = w3.eth.get_transaction_count(account)

        # 部署交易
        tx_hash = w3.eth.send_transaction({
            'from': account,
            'data': bin_code,
            'gas': 10000000,
            'nonce': nonce
        })

        print(f"  交易哈希: {tx_hash.hex()}")

        # 等待交易确认
        print("  等待确认...")
        receipt = w3.eth.wait_for_transaction_receipt(tx_hash, timeout=120)

        if receipt.status == 1:
            address = receipt.contractAddress
            print(f"  ✓ 部署成功!")
            print(f"  合约地址: {address}")
            return address
        else:
            print("  ✗ 部署失败")
            return None

    except Exception as e:
        print(f"  ✗ 部署错误: {e}")
        return None

def main():
    print("=" * 60)
    print("  FISCO BCOS 智能合约部署器")
    print("=" * 60)
    print()

    # 合约列表
    contracts = [
        ("BatchRegistry", "批次登记合约"),
        ("FeedRecord", "饲料记录合约"),
        ("VetRecord", "兽医记录合约"),
        ("InspectionRecord", "检验记录合约"),
        ("TransferRecord", "流转记录合约")
    ]

    # 检查连接
    print("检查节点连接...")
    w3 = check_connection()
    if not w3:
        print("\n无法连接到 FISCO BCOS 节点!")
        print("请确保节点正在运行且 RPC 已启用")
        return

    print()
    print("-" * 60)
    print("开始部署智能合约")
    print("-" * 60)
    print()

    deployed = {}

    for name, desc in contracts:
        print(f"【{desc}】")
        print(f"  合约名: {name}")

        abi, bin_code = load_contract(name)
        if abi and bin_code:
            address = deploy_contract(w3, name, abi, bin_code)
            if address:
                deployed[name] = address
        else:
            print(f"  ✗ 合约文件不存在")

        print()

    # 输出结果
    print("=" * 60)
    print("  部署完成")
    print("=" * 60)
    print()

    if deployed:
        print("已部署的合约:")
        for name, address in deployed.items():
            print(f"  {name}: {address}")

        print()
        print("请将以下配置填入 application.yml:")
        print("-" * 60)
        for name, address in deployed.items():
            key = name.lower()
            print(f"  {key}: {address}")

        # 保存到文件
        try:
            with open("/mnt/c/Users/24835/Desktop/F/deployed-addresses.txt", "w") as f:
                f.write("# 智能合约部署地址\n")
                f.write("contract:\n")
                for name, address in deployed.items():
                    key = name.lower()
                    f.write(f"  {key}: {address}\n")
            print()
            print("配置已保存到: deployed-addresses.txt")
        except Exception as e:
            print(f"保存配置失败: {e}")
    else:
        print("没有合约部署成功")

if __name__ == "__main__":
    main()
