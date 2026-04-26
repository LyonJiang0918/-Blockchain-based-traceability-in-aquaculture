#!/bin/bash
# 更新 FISCO 节点配置

for i in 1 2 3; do
  sed "s/20200/2020$i/g; s/30300/3030$i/g" ~/fisco/nodes/127.0.0.1/node0/config.ini > ~/fisco/nodes/127.0.0.1/node$i/config.ini
done
echo "Configs updated for node1-3"
