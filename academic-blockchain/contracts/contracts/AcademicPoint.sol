// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * AcademicPoint — 学术成果共享激励积分（ERC20）
 *
 * 激励逻辑：当用户 A 向用户 B 授权访问其学术成果（AccessControlManager.grantAccess）后，
 *           系统自动给 A mint 1 个 ACP，鼓励分享。
 *
 * 安全约束：
 *   - mint 仅授权给"经过 owner 添加的 minter"地址（一般是 AccessControlManager 合约）。
 *   - 部署者为 owner，可以增减 minter；最终生产可放弃 ownership 实现去中心化。
 */
contract AcademicPoint is ERC20, Ownable {
    mapping(address => bool) public minters;

    event MinterAdded(address indexed minter);
    event MinterRemoved(address indexed minter);
    event Rewarded(address indexed to, uint256 amount, string reason);

    constructor() ERC20("Academic Point", "ACP") Ownable() {}

    modifier onlyMinter() {
        require(minters[msg.sender], "Caller is not a minter");
        _;
    }

    function addMinter(address minter) external onlyOwner {
        require(minter != address(0), "minter cannot be zero address");
        minters[minter] = true;
        emit MinterAdded(minter);
    }

    function removeMinter(address minter) external onlyOwner {
        minters[minter] = false;
        emit MinterRemoved(minter);
    }

    /** 由白名单 minter 调用，给指定地址增发积分。 */
    function reward(address to, uint256 amount, string calldata reason) external onlyMinter {
        require(to != address(0), "to cannot be zero address");
        _mint(to, amount);
        emit Rewarded(to, amount, reason);
    }
}
