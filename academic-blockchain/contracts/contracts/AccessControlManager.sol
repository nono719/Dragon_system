// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./RecordRegistry.sol";
import "./AcademicPoint.sol";

/**
 * AccessControlManager — 学术成果共享授权管理合约（论文 §4.2.2 改造版）
 *
 * 关键性质：
 *   1. 授权操作必须由该 recordId 当前的 NFT 持有者发起（与 RecordRegistry 联动校验）。
 *   2. 支持有效期（expireTime = 0 表示长期有效）。
 *   3. 链上 + 链下双写：合约负责真值，后端数据库做镜像便于查询。
 *   4. 激励：每次成功 grantAccess 后给 grantor mint `rewardPerGrant` 个 ACP（积分合约可选）。
 */
contract AccessControlManager {
    struct AuthorizationInfo {
        address owner;
        string permissionType;
        uint256 expireTime;
        bool active;
    }

    RecordRegistry public immutable registry;
    AcademicPoint  public immutable incentive;       // 可为 zero 地址表示不启用积分
    uint256        public immutable rewardPerGrant;  // 每次授权奖励的积分（含 18 位小数）

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

    constructor(address registryAddress, address incentiveAddress, uint256 rewardAmount) {
        require(registryAddress != address(0), "registry address cannot be zero");
        registry = RecordRegistry(registryAddress);
        incentive = AcademicPoint(incentiveAddress); // 允许 zero
        rewardPerGrant = rewardAmount;
    }

    modifier onlyRecordOwner(uint256 recordId) {
        require(registry.currentOwner(recordId) == msg.sender, "Only record owner can operate");
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

        // 激励：try 模式，积分合约调用失败不影响主流程
        if (address(incentive) != address(0) && rewardPerGrant > 0) {
            try incentive.reward(msg.sender, rewardPerGrant, "grantAccess") {
                // success
            } catch {
                // ignore — token contract may be paused / minter revoked
            }
        }
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
