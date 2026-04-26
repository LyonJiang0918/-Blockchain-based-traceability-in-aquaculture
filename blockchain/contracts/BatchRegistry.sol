// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.6.10;

/**
 * @title 批次上链登记合约
 * @dev 养殖批次信息上链登记
 */
contract BatchRegistry {

    struct BatchInfo {
        string batchId;
        string farmId;
        string species;
        int256 quantity;
        string location;
        uint256 status;
        uint256 createdAt;
        address creater;
    }

    mapping(string => BatchInfo) public batches;
    string[] public batchIds;

    event BatchCreated(string batchId, string farmId, string species, int256 quantity, address creater);
    event BatchStatusUpdated(string batchId, uint256 oldStatus, uint256 newStatus);

    function createBatch(
        string memory batchId,
        string memory farmId,
        string memory species,
        int256 quantity,
        string memory location
    ) public returns (bool) {
        require(bytes(batchId).length > 0, "batchId cannot be empty");
        require(bytes(batches[batchId].batchId).length == 0, "batch already exists");

        batches[batchId] = BatchInfo({
            batchId: batchId,
            farmId: farmId,
            species: species,
            quantity: quantity,
            location: location,
            status: 0,
            createdAt: block.timestamp,
            creater: msg.sender
        });
        batchIds.push(batchId);

        emit BatchCreated(batchId, farmId, species, quantity, msg.sender);
        return true;
    }

    function updateStatus(string memory batchId, uint256 newStatus) public returns (bool) {
        require(bytes(batches[batchId].batchId).length > 0, "batch not found");

        uint256 oldStatus = batches[batchId].status;
        batches[batchId].status = newStatus;

        emit BatchStatusUpdated(batchId, oldStatus, newStatus);
        return true;
    }

    function getBatch(string memory batchId) public view returns (
        string memory,
        string memory,
        string memory,
        int256,
        string memory,
        uint256,
        uint256,
        address
    ) {
        BatchInfo memory b = batches[batchId];
        return (
            b.batchId,
            b.farmId,
            b.species,
            b.quantity,
            b.location,
            b.status,
            b.createdAt,
            b.creater
        );
    }

    function exists(string memory batchId) public view returns (bool) {
        return bytes(batches[batchId].batchId).length > 0;
    }

    function getBatchCount() public view returns (uint256) {
        return batchIds.length;
    }
}
