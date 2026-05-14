// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import "@openzeppelin/contracts/utils/Base64.sol";
import "@openzeppelin/contracts/utils/Strings.sol";

/**
 * RecordRegistry — 学术成果链上存证 + ERC721 NFT 凭证合约（论文 §4.2.1 改造版）
 *
 * 改造目标：把存证记录改造为 NFT，每条成果存证铸造一枚 ERC721 token 给所有者；
 *           tokenId 与 recordId 一一对应，钱包（如 MetaMask）的 NFTs 标签可直接看到。
 *
 * 数据流：
 *   1. 后端/前端对成果文件做 SHA-256 → bytes32 fileHash
 *   2. 对成果元数据做 keccak256 → bytes32 metadataHash
 *   3. 调用 registerRecord(fileHash, metadataHash) 由 msg.sender 接收 NFT
 *   4. 合约校验 fileHash 唯一 → 分配 recordId / tokenId → mint NFT
 *   5. tokenURI 返回 data:application/json;base64,... 直接内联，不依赖外部存储
 */
contract RecordRegistry is ERC721URIStorage {
    using Strings for uint256;

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

    constructor() ERC721("Academic Achievement NFT", "AAN") {}

    function registerRecord(bytes32 fileHash, bytes32 metadataHash)
        external
        returns (uint256)
    {
        require(fileHash != bytes32(0), "File hash cannot be empty");
        require(recordIdByHash[fileHash] == 0, "Record already exists");

        currentRecordId++;
        uint256 id = currentRecordId;
        records[id] = RecordInfo({
            recordId: id,
            fileHash: fileHash,
            metadataHash: metadataHash,
            ownerAddress: msg.sender,
            recordTime: block.timestamp,
            exists: true
        });
        recordIdByHash[fileHash] = id;

        _safeMint(msg.sender, id);
        _setTokenURI(id, _buildTokenURI(id, fileHash, metadataHash, msg.sender, block.timestamp));

        emit RecordRegistered(id, fileHash, metadataHash, msg.sender, block.timestamp);
        return id;
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

    /**
     * 当前 NFT 的实际持有者（ERC721 ownerOf），与最初存证者 `recordInfo.ownerAddress`
     * 可能不同（如果用户后续把 NFT 转给了别人）。AccessControlManager 在校验授权
     * 操作权限时使用此函数。
     */
    function currentOwner(uint256 recordId) external view returns (address) {
        require(records[recordId].exists, "Record does not exist");
        return ownerOf(recordId);
    }

    // ---------------------------------------------------------------------
    // NFT 元数据（链上内联 JSON）
    // ---------------------------------------------------------------------

    function _buildTokenURI(
        uint256 id,
        bytes32 fileHash,
        bytes32 metadataHash,
        address owner,
        uint256 timestamp
    ) internal pure returns (string memory) {
        bytes memory json = abi.encodePacked(
            '{"name":"Academic Achievement #', id.toString(),
            '","description":"Blockchain-based academic achievement registration certificate",',
            '"attributes":[',
                '{"trait_type":"recordId","value":"', id.toString(), '"},',
                '{"trait_type":"fileHash","value":"', _toHexString(fileHash), '"},',
                '{"trait_type":"metadataHash","value":"', _toHexString(metadataHash), '"},',
                '{"trait_type":"owner","value":"', _toAddressString(owner), '"},',
                '{"trait_type":"recordTime","value":"', timestamp.toString(), '"}',
            ']}'
        );
        return string(
            abi.encodePacked("data:application/json;base64,", Base64.encode(json))
        );
    }

    function _toHexString(bytes32 value) internal pure returns (string memory) {
        bytes memory alphabet = "0123456789abcdef";
        bytes memory buffer = new bytes(66);
        buffer[0] = "0";
        buffer[1] = "x";
        for (uint256 i = 0; i < 32; i++) {
            buffer[2 + i * 2]     = alphabet[uint8(value[i] >> 4)];
            buffer[3 + i * 2]     = alphabet[uint8(value[i] & 0x0f)];
        }
        return string(buffer);
    }

    function _toAddressString(address addr) internal pure returns (string memory) {
        return Strings.toHexString(uint256(uint160(addr)), 20);
    }
}
