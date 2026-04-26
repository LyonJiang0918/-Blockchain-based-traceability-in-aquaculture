#!/bin/bash
# FISCO BCOS 节点启动脚本
# 使用方法: bash start_nodes.sh

NODES_DIR="$HOME/fisco/nodes/127.0.0.1"
FISCO_BINARY="$HOME/fisco/fisco-bcos"

echo "FISCO BCOS 节点启动脚本"
echo "====================="

# 检查二进制文件
if [ ! -f "$FISCO_BINARY" ]; then
    echo "错误: 找不到 fisco-bcos 二进制文件: $FISCO_BINARY"
    exit 1
fi

# 停止现有节点
echo "停止现有节点..."
pkill -9 fisco-bcos 2>/dev/null
sleep 2

# 启动所有节点
echo "启动节点..."

for i in 0 1 2 3; do
    NODE_DIR="$NODES_DIR/node$i"
    if [ -d "$NODE_DIR" ]; then
        cd "$NODE_DIR"
        nohup $FISCO_BINARY -c config.ini -g config.genesis > /dev/null 2>&1 &
        echo "  - node$i 已启动"
    else
        echo "  - 警告: node$i 目录不存在"
    fi
done

sleep 3

# 检查状态
RUNNING=$(ps aux | grep fisco-bcos | grep -v grep | wc -l)
echo ""
echo "====================="
echo "当前运行节点数: $RUNNING"
echo ""

if [ $RUNNING -ge 1 ]; then
    echo "节点启动成功！"
    echo ""
    echo "RPC 端点:"
    echo "  - Node0: http://127.0.0.1:20200"
    echo "  - Node1: http://127.0.0.1:20201"
    echo "  - Node2: http://127.0.0.1:20202"
    echo "  - Node3: http://127.0.0.1:20203"
else
    echo "节点启动失败，请检查日志"
fi
