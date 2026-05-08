// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * RecordRegistry — 学术成果链上存证主合约
 * 论文 §4.2.1
 *
 * 数据流：
 *   1. 后端/前端对成果文件做 SHA-256 → bytes32 fileHash
 *   2. 对成果元数据（标题/作者/类型/创建时间...）做哈希 → bytes32 metadataHash
 *   3. 调用 registerRecord(fileHash, metadataHash)
 *   4. 合约校验 fileHash 是否已存在；若不存在则分配自增 recordId 并写入区块链
 *   5. 通过事件 RecordRegistered 把上链结果广播给前端 / 后端监听器
 */
contract RecordRegistry {
    struct RecordInfo {
        uint256 recordId;
        bytes32 fileHash;
        bytes32 metadataHash;
        address ownerAddress;
        uint256 recordTime;
        bool exists;
    }

    mapping(bytes32 => uint256) private recordIdByHash;
    mapping(uint256 => RecordInfo) private records;
    uint256 private currentRecordId;

    event RecordRegistered(
        uint256 indexed recordId,
        bytes32 indexed fileHash,
        bytes32 metadataHash,
        address indexed owner,
        uint256 recordTime
    );

    function registerRecord(bytes32 fileHash, bytes32 metadataHash)
        external
        returns (uint256)
    {
        require(fileHash != bytes32(0), "File hash cannot be empty");
        require(recordIdByHash[fileHash] == 0, "Record already exists");

        currentRecordId++;
        records[currentRecordId] = RecordInfo({
            recordId: currentRecordId,
            fileHash: fileHash,
            metadataHash: metadataHash,
            ownerAddress: msg.sender,
            recordTime: block.timestamp,
            exists: true
        });
        recordIdByHash[fileHash] = currentRecordId;

        emit RecordRegistered(currentRecordId, fileHash, metadataHash, msg.sender, block.timestamp);
        return currentRecordId;
    }

    function getRecordByHash(bytes32 fileHash) external view returns (uint256) {
        return recordIdByHash[fileHash];
    }

    function getRecordInfo(uint256 recordId)
        external
        view
        returns (
            bytes32 fileHash,
            bytes32 metadataHash,
            address ownerAddress,
            uint256 recordTime
        )
    {
        require(records[recordId].exists, "Record does not exist");
        RecordInfo memory r = records[recordId];
        return (r.fileHash, r.metadataHash, r.ownerAddress, r.recordTime);
    }

    function exists(bytes32 fileHash) external view returns (bool) {
        return recordIdByHash[fileHash] != 0;
    }

    function totalRecords() external view returns (uint256) {
        return currentRecordId;
    }
}
