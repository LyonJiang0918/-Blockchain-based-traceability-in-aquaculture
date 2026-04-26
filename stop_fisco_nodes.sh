#!/bin/bash
# FISCO BCOS 节点停止脚本

echo "停止 FISCO BCOS 节点..."
pkill -f fisco-bcos 2>/dev/null
sleep 2

RUNNING=$(ps aux | grep fisco-bcos | grep -v grep | wc -l)
if [ $RUNNING -eq 0 ]; then
    echo "所有节点已停止"
else
    echo "仍有 $RUNNING 个进程运行，强制停止..."
    pkill -9 fisco-bcos 2>/dev/null
fi
