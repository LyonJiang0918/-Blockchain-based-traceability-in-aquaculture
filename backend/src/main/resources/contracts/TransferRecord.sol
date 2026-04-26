pragma solidity ^0.6.10;

/**
 * @title 流转记录合约
 * @dev 记录批次在不同环节间的流转
 */
contract TransferRecord {

    struct TransferInfo {
        string batchId;
        uint8 fromStage;
        uint8 toStage;
        string toParty;
        uint256 transferDate;
        uint256 quantity;
        uint256 createdAt;
    }

    mapping(string => TransferInfo[]) public batchTransfers;

    event TransferRecordCreated(string indexed recordId, string indexed batchId, uint8 toStage);

    function createTransferRecord(
        string memory recordId,
        string memory batchId,
        uint8 fromStage,
        uint8 toStage,
        string memory toParty,
        uint256 transferDate,
        uint256 quantity
    ) public {
        batchTransfers[batchId].push(TransferInfo({
            batchId: batchId,
            fromStage: fromStage,
            toStage: toStage,
            toParty: toParty,
            transferDate: transferDate,
            quantity: quantity,
            createdAt: block.timestamp
        }));
        emit TransferRecordCreated(recordId, batchId, toStage);
    }

    function getBatchTransferCount(string memory batchId) public view returns (uint256) {
        return batchTransfers[batchId].length;
    }

    function getBatchTransfer(string memory batchId, uint256 index) public view returns (
        string memory,
        uint8,
        uint8,
        string memory,
        uint256,
        uint256,
        uint256
    ) {
        TransferInfo memory t = batchTransfers[batchId][index];
        return (t.batchId, t.fromStage, t.toStage, t.toParty, t.transferDate, t.quantity, t.createdAt);
    }
}
