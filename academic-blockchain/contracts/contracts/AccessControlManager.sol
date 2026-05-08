// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./RecordRegistry.sol";

/**
 * AccessControlManager — 学术成果共享授权管理合约
 * 论文 §4.2.2
 *
 * 关键性质：
 *   1. 授权操作必须由该 recordId 的成果所有者发起（与 RecordRegistry 联动校验）
 *   2. 支持有效期（expireTime = 0 表示长期有效）
 *   3. 链上 + 链下双写：合约负责真值，后端数据库做镜像便于查询
 */
contract AccessControlManager {
    struct AuthorizationInfo {
        address owner;
        string permissionType; // 例如: "READ" / "DOWNLOAD"
        uint256 expireTime;    // unix 秒；0 表示永久
        bool active;
    }

    RecordRegistry public immutable registry;

    // recordId => grantee => AuthorizationInfo
    mapping(uint256 => mapping(address => AuthorizationInfo)) private authorizations;

    event AccessGranted(
        uint256 indexed recordId,
        address indexed owner,
        address indexed grantee,
        string permissionType,
        uint256 expireTime
    );

    event AccessRevoked(
        uint256 indexed recordId,
        address indexed owner,
        address indexed grantee
    );

    constructor(address registryAddress) {
        require(registryAddress != address(0), "registry address cannot be zero");
        registry = RecordRegistry(registryAddress);
    }

    modifier onlyRecordOwner(uint256 recordId) {
        (, , address ownerAddress, ) = registry.getRecordInfo(recordId);
        require(ownerAddress == msg.sender, "Only record owner can operate");
        _;
    }

    function grantAccess(
        uint256 recordId,
        address grantee,
        string memory permissionType,
        uint256 expireTime
    ) external onlyRecordOwner(recordId) {
        require(grantee != address(0), "Invalid grantee address");
        require(grantee != msg.sender, "Cannot grant to yourself");
        require(
            expireTime == 0 || expireTime > block.timestamp,
            "expireTime must be in the future or zero"
        );

        authorizations[recordId][grantee] = AuthorizationInfo({
            owner: msg.sender,
            permissionType: permissionType,
            expireTime: expireTime,
            active: true
        });

        emit AccessGranted(recordId, msg.sender, grantee, permissionType, expireTime);
    }

    function revokeAccess(uint256 recordId, address grantee)
        external
        onlyRecordOwner(recordId)
    {
        require(authorizations[recordId][grantee].active, "Access not granted");
        authorizations[recordId][grantee].active = false;
        emit AccessRevoked(recordId, msg.sender, grantee);
    }

    function checkAccess(uint256 recordId, address user) external view returns (bool) {
        AuthorizationInfo memory auth = authorizations[recordId][user];
        if (!auth.active) return false;
        if (auth.expireTime != 0 && block.timestamp > auth.expireTime) return false;
        return true;
    }

    function getAuthorization(uint256 recordId, address grantee)
        external
        view
        returns (
            address owner,
            string memory permissionType,
            uint256 expireTime,
            bool active
        )
    {
        AuthorizationInfo memory auth = authorizations[recordId][grantee];
        return (auth.owner, auth.permissionType, auth.expireTime, auth.active);
    }
}
