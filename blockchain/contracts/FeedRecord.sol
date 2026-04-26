// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.6.10;

/**
 * @title 饲料记录合约
 * @dev 记录饲料投喂信息
 */
contract FeedRecord {

    struct FeedInfo {
        string batchId;
        string feedType;
        uint256 feedDate;
        uint256 amount;
        uint256 createdAt;
    }

    mapping(string => FeedInfo[]) public batchFeeds;

    event FeedRecordCreated(string indexed recordId, string indexed batchId, uint256 feedDate);

    function createFeedRecord(
        string memory recordId,
        string memory batchId,
        string memory feedType,
        uint256 feedDate,
        uint256 amount
    ) public {
        batchFeeds[batchId].push(FeedInfo({
            batchId: batchId,
            feedType: feedType,
            feedDate: feedDate,
            amount: amount,
            createdAt: block.timestamp
        }));
        emit FeedRecordCreated(recordId, batchId, feedDate);
    }

    function getBatchFeedCount(string memory batchId) public view returns (uint256) {
        return batchFeeds[batchId].length;
    }

    function getBatchFeed(string memory batchId, uint256 index) public view returns (
        string memory,
        string memory,
        uint256,
        uint256,
        uint256
    ) {
        FeedInfo memory f = batchFeeds[batchId][index];
        return (f.batchId, f.feedType, f.feedDate, f.amount, f.createdAt);
    }
}
