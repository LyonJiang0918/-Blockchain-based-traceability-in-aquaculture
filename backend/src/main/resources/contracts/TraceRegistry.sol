pragma solidity ^0.6.10;

contract TraceRegistry {

    event BatchCreated(string batchId, string farmId, string species, int256 quantity, string location, address operator, uint256 timestamp, string metaHash);
    event BatchStatusUpdated(string batchId, uint256 oldStatus, uint256 newStatus, address operator, uint256 timestamp, string metaHash);
    event AdminRollback(string batchId, uint256 fromStatus, uint256 toStatus, address operator, string reason, uint256 timestamp, string metaHash);

    event ProcessStarted(string recordId, string batchId, string processType, uint256 inputCount, address operator, uint256 timestamp, string metaHash);
    event ProcessCompleted(string recordId, string batchId, string processType, uint256 outputCount, address operator, uint256 timestamp, string metaHash);

    event FeedRecordCreated(string recordId, string groupId, string feedType, string feedBatchId, string feedBrand, uint256 amount, string feedingMethod, address operator, uint256 timestamp, string metaHash);
    event VetRecordCreated(string recordId, string groupId, uint256 recordType, string vetId, string vetName, string drugName, string vaccineName, string dosage, string result, address operator, uint256 timestamp, string metaHash);
    event InspectionRecordCreated(string recordId, string batchId, string inspectorId, string inspectorName, uint256 inspectDate, uint256 result, string reportHash, address operator, uint256 timestamp, string metaHash);
    event TransferRecordCreated(string recordId, string batchId, uint256 fromStage, uint256 toStage, string fromParty, string toParty, uint256 transferDate, uint256 quantity, address operator, uint256 timestamp, string metaHash);

    function createBatch(string memory batchId, string memory farmId, string memory species, int256 quantity, string memory location, string memory metaHash) public {
        emit BatchCreated(batchId, farmId, species, quantity, location, msg.sender, block.timestamp, metaHash);
    }

    function updateBatchStatus(string memory batchId, uint256 oldStatus, uint256 newStatus, string memory metaHash) public {
        emit BatchStatusUpdated(batchId, oldStatus, newStatus, msg.sender, block.timestamp, metaHash);
    }

    function adminRollback(string memory batchId, uint256 fromStatus, uint256 toStatus, string memory reason, string memory metaHash) public {
        emit AdminRollback(batchId, fromStatus, toStatus, msg.sender, reason, block.timestamp, metaHash);
    }

    function startProcess(string memory recordId, string memory batchId, string memory processType, uint256 inputCount, string memory metaHash) public {
        emit ProcessStarted(recordId, batchId, processType, inputCount, msg.sender, block.timestamp, metaHash);
    }

    function completeProcess(string memory recordId, string memory batchId, string memory processType, uint256 outputCount, string memory metaHash) public {
        emit ProcessCompleted(recordId, batchId, processType, outputCount, msg.sender, block.timestamp, metaHash);
    }

    function createFeedRecord(string memory recordId, string memory groupId, string memory feedType, string memory feedBatchId, string memory feedBrand, uint256 amount, string memory feedingMethod, string memory metaHash) public {
        emit FeedRecordCreated(recordId, groupId, feedType, feedBatchId, feedBrand, amount, feedingMethod, msg.sender, block.timestamp, metaHash);
    }

    function createVetRecord(string memory recordId, string memory groupId, uint256 recordType, string memory vetId, string memory vetName, string memory drugName, string memory vaccineName, string memory dosage, string memory result, string memory metaHash) public {
        emit VetRecordCreated(recordId, groupId, recordType, vetId, vetName, drugName, vaccineName, dosage, result, msg.sender, block.timestamp, metaHash);
    }

    function createInspectionRecord(string memory recordId, string memory batchId, string memory inspectorId, string memory inspectorName, uint256 inspectDate, uint256 result, string memory reportHash, string memory metaHash) public {
        emit InspectionRecordCreated(recordId, batchId, inspectorId, inspectorName, inspectDate, result, reportHash, msg.sender, block.timestamp, metaHash);
    }

    function createTransferRecord(string memory recordId, string memory batchId, uint256 fromStage, uint256 toStage, string memory fromParty, string memory toParty, uint256 transferDate, uint256 quantity, string memory metaHash) public {
        emit TransferRecordCreated(recordId, batchId, fromStage, toStage, fromParty, toParty, transferDate, quantity, msg.sender, block.timestamp, metaHash);
    }
}
