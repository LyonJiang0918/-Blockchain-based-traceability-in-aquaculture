const solc = require('solc');
const fs = require('fs');
const path = require('path');

const contractDir = path.join(__dirname, 'contracts');
const outputDir = path.join(__dirname, 'contracts', 'compiled');

// 创建输出目录
if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
}

// 编译所有合约
const contracts = ['BatchRegistry', 'FeedRecord', 'VetRecord', 'InspectionRecord', 'TransferRecord'];

console.log('=== 智能合约编译器 ===\n');
console.log('合约目录:', contractDir);
console.log('输出目录:', outputDir);
console.log('');

contracts.forEach(contractName => {
    const contractPath = path.join(contractDir, contractName + '.sol');
    console.log('编译:', contractName);

    try {
        const source = fs.readFileSync(contractPath, 'utf8');

        const input = {
            language: 'Solidity',
            sources: {
                [contractName + '.sol']: {
                    content: source
                }
            },
            settings: {
                outputSelection: {
                    '*': {
                        '*': ['*']
                    }
                }
            }
        };

        const output = JSON.parse(solc.compile(JSON.stringify(input)));

        if (output.errors) {
            console.log('  错误:');
            output.errors.forEach(e => console.log('    ' + e.formattedMessage));
            return;
        }

        const contract = output.contracts[contractName + '.sol'][contractName];

        // 输出 ABI
        fs.writeFileSync(
            path.join(outputDir, contractName + '.abi'),
            JSON.stringify(contract.abi, null, 2)
        );

        // 输出 BIN
        if (contract.evm && contract.evm.bytecode) {
            fs.writeFileSync(
                path.join(outputDir, contractName + '.bin'),
                contract.evm.bytecode.object
            );
        }

        console.log('  成功! ABI 和 BIN 已生成');
    } catch (err) {
        console.log('  失败:', err.message);
    }
});

console.log('\n=== 编译完成 ===');
