// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.6.10;

/**
 * @title 检验记录合约
 * @dev 记录第三方检验检测信息
 */
contract InspectionRecord {

    struct InspectionInfo {
        string batchId;
        string inspectorName;
        uint256 inspectDate;
        uint8 result;
        uint256 createdAt;
    }

    mapping(string => InspectionInfo[]) public batchInspections;

    event InspectionRecordCreated(string indexed recordId, string indexed batchId, uint8 result);

    function createInspectionRecord(
        string memory recordId,
        string memory batchId,
        string memory inspectorName,
        uint256 inspectDate,
        uint8 result
    ) public {
        batchInspections[batchId].push(InspectionInfo({
            batchId: batchId,
            inspectorName: inspectorName,
            inspectDate: inspectDate,
            result: result,
            createdAt: block.timestamp
        }));
        emit InspectionRecordCreated(recordId, batchId, result);
    }

    function getBatchInspectionCount(string memory batchId) public view returns (uint256) {
        return batchInspections[batchId].length;
    }

    function getBatchInspection(string memory batchId, uint256 index) public view returns (
        string memory,
        string memory,
        uint256,
        uint8,
        uint256
    ) {
        InspectionInfo memory i = batchInspections[batchId][index];
        return (i.batchId, i.inspectorName, i.inspectDate, i.result, i.createdAt);
    }
}
