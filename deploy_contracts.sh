#!/bin/bash
# FISCO BCOS 一键智能合约部署脚本
# 使用方法: bash deploy_contracts.sh

echo "=============================================="
echo "  FISCO BCOS 智能合约部署"
echo "=============================================="
echo ""

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "错误: 未安装 Java"
    echo ""
    echo "请先安装 Java:"
    echo "  sudo apt update && sudo apt install -y openjdk-17-jdk"
    echo ""
    exit 1
fi

echo "Java 版本:"
java -version 2>&1 | head -1
echo ""

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未安装 Maven"
    echo ""
    echo "请先安装 Maven:"
    echo "  sudo apt install -y maven"
    echo ""
    exit 1
fi

echo "Maven 已安装"
echo ""

# 检查 FISCO 节点
echo "检查 FISCO BCOS 节点..."
NODE_COUNT=$(ps aux | grep fisco-bcos | grep -v grep | wc -l)
if [ "$NODE_COUNT" -lt 1 ]; then
    echo "警告: FISCO BCOS 节点未运行"
    echo "请先启动节点: cd ~/fisco/nodes/127.0.0.1 && bash start_all.sh"
    echo ""
else
    echo "✓ $NODE_COUNT 个节点正在运行"
fi
echo ""

# 项目路径
PROJECT_DIR="/mnt/c/Users/24835/Desktop/F/backend"

if [ ! -d "$PROJECT_DIR" ]; then
    echo "错误: 找不到项目目录 $PROJECT_DIR"
    echo "请检查路径是否正确"
    exit 1
fi

echo "项目目录: $PROJECT_DIR"
echo ""

# 检查合约文件
echo "检查合约文件..."
CONTRACT_DIR="$PROJECT_DIR/src/main/resources/contracts/compiled"

if [ ! -d "$CONTRACT_DIR" ]; then
    echo "错误: 找不到合约目录"
    exit 1
fi

CONTRACTS=("BatchRegistry" "FeedRecord" "VetRecord" "InspectionRecord" "TransferRecord")
for c in "${CONTRACTS[@]}"; do
    if [ ! -f "$CONTRACT_DIR/$c.abi" ] || [ ! -f "$CONTRACT_DIR/$c.bin" ]; then
        echo "错误: 缺少 $c 合约文件"
        exit 1
    fi
done
echo "✓ 所有合约文件存在"
echo ""

# 编译并部署
echo "开始部署..."
cd "$PROJECT_DIR"

echo "1. 编译项目..."
mvn clean compile -q -DskipTests 2>/dev/null

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误"
    exit 1
fi
echo "✓ 编译成功"
echo ""

echo "2. 运行部署程序..."
mvn exec:java -Dexec.mainClass="com.trace.contract.ContractDeployer" -Dexec.cleanupDaemonThreads=false 2>&1 | tail -50

echo ""
echo "=============================================="
echo "  部署完成"
echo "=============================================="
echo ""
echo "检查 deployed-addresses.txt 文件获取合约地址"
echo "然后将地址填入 application.yml 的 contract 配置项"
