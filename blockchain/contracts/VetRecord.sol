// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.6.10;

/**
 * @title 兽医记录合约
 * @dev 记录免疫、用药等兽医操作
 */
contract VetRecord {

    struct VetInfo {
        string batchId;
        uint8 recordType;
        string medicineName;
        uint256 operationDate;
        uint256 dosage;
        uint256 createdAt;
    }

    mapping(string => VetInfo[]) public batchVetRecords;

    event VetRecordCreated(string indexed recordId, string indexed batchId, uint8 recordType);

    function createVetRecord(
        string memory recordId,
        string memory batchId,
        uint8 recordType,
        string memory medicineName,
        uint256 operationDate,
        uint256 dosage
    ) public {
        batchVetRecords[batchId].push(VetInfo({
            batchId: batchId,
            recordType: recordType,
            medicineName: medicineName,
            operationDate: operationDate,
            dosage: dosage,
            createdAt: block.timestamp
        }));
        emit VetRecordCreated(recordId, batchId, recordType);
    }

    function getBatchVetCount(string memory batchId) public view returns (uint256) {
        return batchVetRecords[batchId].length;
    }

    function getBatchVet(string memory batchId, uint256 index) public view returns (
        string memory,
        uint8,
        string memory,
        uint256,
        uint256,
        uint256
    ) {
        VetInfo memory v = batchVetRecords[batchId][index];
        return (v.batchId, v.recordType, v.medicineName, v.operationDate, v.dosage, v.createdAt);
    }
}
