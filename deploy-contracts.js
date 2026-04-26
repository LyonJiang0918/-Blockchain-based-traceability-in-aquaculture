/**
 * FISCO BCOS 智能合约部署脚本
 * 使用 Node.js 内置 http 模块
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

// FISCO BCOS RPC 配置
const RPC_HOST = '127.0.0.1';
const RPC_PORT = 20200;

// 合约目录
const CONTRACT_DIR = path.join(__dirname, 'backend', 'src', 'main', 'resources', 'contracts', 'compiled');

// 合约列表
const CONTRACTS = [
    { name: 'BatchRegistry', desc: '批次登记合约' },
    { name: 'FeedRecord', desc: '饲料记录合约' },
    { name: 'VetRecord', desc: '兽医记录合约' },
    { name: 'InspectionRecord', desc: '检验记录合约' },
    { name: 'TransferRecord', desc: '流转记录合约' }
];

// RPC 调用
function callRpc(method, params = []) {
    return new Promise((resolve, reject) => {
        const data = JSON.stringify({
            jsonrpc: '2.0',
            method: method,
            params: params,
            id: 1
        });

        const options = {
            hostname: RPC_HOST,
            port: RPC_PORT,
            path: '/',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': data.length
            }
        };

        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try {
                    const result = JSON.parse(body);
                    resolve(result);
                } catch (e) {
                    reject(new Error('Invalid JSON response'));
                }
            });
        });

        req.on('error', reject);
        req.setTimeout(10000, () => {
            req.destroy();
            reject(new Error('Request timeout'));
        });

        req.write(data);
        req.end();
    });
}

// 获取账户
async function getAccounts() {
    const result = await callRpc('eth_accounts');
    return result.result || [];
}

// 获取交易计数
async function getTransactionCount(account) {
    const result = await callRpc('eth_getTransactionCount', [account, 'latest']);
    return result.result || '0x0';
}

// 获取区块 gas limit
async function getBlockGasLimit() {
    const result = await callRpc('eth_getBlockByNumber', ['latest', false]);
    if (result.result && result.result.gasLimit) {
        return result.result.gasLimit;
    }
    return '0x1000000';
}

// 发送交易
async function sendTransaction(tx) {
    return await callRpc('eth_sendTransaction', [tx]);
}

// 获取交易收据
async function getTransactionReceipt(txHash) {
    return await callRpc('eth_getTransactionReceipt', [txHash]);
}

// 等待交易确认
async function waitForReceipt(txHash, timeout = 60) {
    for (let i = 0; i < timeout; i++) {
        await new Promise(resolve => setTimeout(resolve, 2000));
        const receipt = await getTransactionReceipt(txHash);
        if (receipt.result && receipt.result.contractAddress) {
            return receipt.result;
        }
        process.stdout.write('.');
    }
    return null;
}

// 部署合约
async function deployContract(account, bin) {
    const nonce = await getTransactionCount(account);
    const gasLimit = await getBlockGasLimit();

    console.log(`  账户: ${account}`);
    console.log(`  Nonce: ${nonce}`);
    console.log(`  Gas Limit: ${gasLimit}`);

    const tx = {
        from: account,
        data: '0x' + bin,
        gas: gasLimit,
        nonce: nonce
    };

    console.log('  发送部署交易...');
    const result = await sendTransaction(tx);

    if (result.error) {
        throw new Error(result.error.message);
    }

    const txHash = result.result;
    console.log(`  交易哈希: ${txHash}`);

    console.log('  等待确认');
    const receipt = await waitForReceipt(txHash);

    if (receipt) {
        return receipt.contractAddress;
    }
    return null;
}

// 主函数
async function main() {
    console.log('='.repeat(60));
    console.log('  FISCO BCOS 智能合约部署器');
    console.log('='.repeat(60));
    console.log();

    // 检查合约目录
    if (!fs.existsSync(CONTRACT_DIR)) {
        console.error(`错误: 找不到合约目录 ${CONTRACT_DIR}`);
        return;
    }

    // 检查连接
    console.log('检查节点连接...');
    try {
        const blockResult = await callRpc('eth_blockNumber');
        if (blockResult.result) {
            console.log(`  ✓ 节点已连接，当前区块: ${parseInt(blockResult.result, 16)}`);
        }
    } catch (e) {
        console.error(`  ✗ 无法连接到节点: ${e.message}`);
        console.error();
        console.error('请确保:');
        console.error('  1. FISCO BCOS 节点正在运行');
        console.error('  2. 节点 RPC 已启用且支持 HTTP');
        return;
    }

    // 获取账户
    console.log();
    console.log('获取部署账户...');
    const accounts = await getAccounts();
    if (accounts.length === 0) {
        console.error('  ✗ 没有可用账户');
        return;
    }
    console.log(`  ✓ 账户: ${accounts[0]}`);

    console.log();
    console.log('-'.repeat(60));
    console.log('开始部署智能合约');
    console.log('-'.repeat(60));
    console.log();

    const deployed = {};

    for (const contract of CONTRACTS) {
        console.log(`【${contract.desc}】`);
        console.log(`  合约名: ${contract.name}`);

        const abiPath = path.join(CONTRACT_DIR, `${contract.name}.abi`);
        const binPath = path.join(CONTRACT_DIR, `${contract.name}.bin`);

        if (!fs.existsSync(abiPath) || !fs.existsSync(binPath)) {
            console.log(`  ✗ 合约文件不存在`);
            console.log();
            continue;
        }

        try {
            const bin = fs.readFileSync(binPath, 'utf8').trim();
            const address = await deployContract(accounts[0], bin);

            if (address) {
                deployed[contract.name] = address;
                console.log();
                console.log(`  ✓ 部署成功!`);
                console.log(`    地址: ${address}`);
            } else {
                console.log();
                console.log(`  ✗ 部署超时`);
            }
        } catch (e) {
            console.log();
            console.log(`  ✗ 部署失败: ${e.message}`);
        }
        console.log();
    }

    // 输出结果
    console.log('='.repeat(60));
    console.log('  部署完成');
    console.log('='.repeat(60));

    if (Object.keys(deployed).length > 0) {
        console.log();
        console.log('已部署的合约:');
        for (const [name, address] of Object.entries(deployed)) {
            console.log(`  ${name}: ${address}`);
        }

        console.log();
        console.log('请将以下配置填入 application.yml:');
        console.log('-'.repeat(50));
        for (const [name, address] of Object.entries(deployed)) {
            const key = name.toLowerCase();
            console.log(`  ${key}: "${address}"`);
        }

        // 保存配置
        const config = `contract:\n${Object.entries(deployed).map(([name, addr]) => `  ${name.toLowerCase()}: "${addr}"`).join('\n')}\n`;
        fs.writeFileSync(path.join(__dirname, 'deployed-addresses.txt'), config);
        console.log();
        console.log('配置已保存到 deployed-addresses.txt');
    } else {
        console.log();
        console.log('没有合约部署成功');
    }
}

main().catch(console.error);
