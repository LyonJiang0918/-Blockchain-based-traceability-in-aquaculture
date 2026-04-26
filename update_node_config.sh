#!/bin/bash
# 更新 FISCO BCOS 节点配置，禁用 SSL

for i in 0 1 2 3; do
  cat > ~/fisco/nodes/127.0.0.1/node$i/config.ini << EOF
[rpc]
    listen_ip = 0.0.0.0
    listen_port = 2020$i
    disallow_trusted_ip = false
    ssl = false

[p2p]
    listen_ip = 0.0.0.0
    listen_port = 3030$i
    nodes = []

[network_security]
    directory = .
    ca_cert = ca.crt
    ssl_cert = ssl.crt
    ssl_key = ssl.key

[storage_security]
    enable = false

[storage]
    type = RocksDB

[tx_pool]
    limit = 150000

[consensus]
    consensus_type = pbft
    leader_period = 1

[log]
    log_path = ./logs
    level = INFO

[chain]
    id = 1

[group]
    id = 1
EOF
  echo "node$i config updated"
done

echo "All configs updated"
