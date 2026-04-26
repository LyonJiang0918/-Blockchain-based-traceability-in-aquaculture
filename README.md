# F 项目

这是一个基于 **Spring Boot + FISCO BCOS** 的养殖溯源系统，包含后端业务接口、智能合约交互、合约部署脚本以及环境辅助工具。

## 项目结构

- `backend/`：Java 后端服务
  - `controller/`：REST 接口
  - `service/`：业务逻辑
  - `dao/`：数据库访问层
  - `entity/`：数据库实体
  - `dto/`：接口数据对象
  - `contract/`：智能合约 Java 封装
  - `resources/contracts/`：Solidity 合约源码与编译产物
- `scripts/`：Python 辅助脚本
  - `deploy_contracts_rpc.py`：通过 RPC 部署合约
  - `deploy_contracts_web3.py`：通过 Web3.py 部署合约
  - `get_fisco_releases.py`：获取 FISCO BCOS release 资源信息
- `start-frontend.sh`：前端启动脚本

## Python 脚本用途

这些脚本主要用于项目部署和环境准备，不是核心业务代码。

### 合约部署
- `scripts/deploy_contracts_rpc.py`
- `scripts/deploy_contracts_web3.py`

用于把编译后的合约部署到 FISCO BCOS 节点，并输出合约地址，方便写入后端配置。

### 发布信息查询
- `scripts/get_fisco_releases.py`

用于查询 FISCO BCOS GitHub release 信息，辅助下载和更新相关组件。

## 依赖

### 后端
- Java 17+
- Maven
- 数据库（按你的 `application.yml` 配置）
- FISCO BCOS 节点

### Python 脚本
- Python 3.8+
- `requests`
- `web3`

可按需安装：

```bash
pip install requests web3
```

## 运行说明

### 后端

进入 `backend/` 后按常规 Maven 方式启动。

### Python 脚本

在项目根目录下运行，例如：

```bash
python scripts/deploy_contracts_rpc.py
```

## 备注

- `backend/target/` 是构建产物，不需要提交到仓库。
- `scripts/` 目录下的脚本属于辅助工具，可以按需保留或扩展。
