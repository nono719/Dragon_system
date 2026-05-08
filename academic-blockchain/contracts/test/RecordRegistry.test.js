const { expect } = require("chai");
const { ethers } = require("hardhat");
const { anyValue } = require("@nomicfoundation/hardhat-chai-matchers/withArgs");

describe("RecordRegistry", function () {
  let registry;
  let owner;
  let other;
  const fileHashA = ethers.id("file-A");        // keccak256("file-A")
  const fileHashB = ethers.id("file-B");
  const metaHashA = ethers.id("meta-A");

  beforeEach(async function () {
    [owner, other] = await ethers.getSigners();
    const RecordRegistry = await ethers.getContractFactory("RecordRegistry");
    registry = await RecordRegistry.deploy();
    await registry.waitForDeployment();
  });

  it("registers a new record and emits event", async function () {
    await expect(registry.registerRecord(fileHashA, metaHashA))
      .to.emit(registry, "RecordRegistered")
      .withArgs(1, fileHashA, metaHashA, owner.address, anyValue);

    expect(await registry.totalRecords()).to.equal(1);
    expect(await registry.exists(fileHashA)).to.be.true;
    expect(await registry.getRecordByHash(fileHashA)).to.equal(1);
  });

  it("rejects empty file hash", async function () {
    await expect(
      registry.registerRecord(ethers.ZeroHash, metaHashA)
    ).to.be.revertedWith("File hash cannot be empty");
  });

  it("rejects duplicate file hash", async function () {
    await registry.registerRecord(fileHashA, metaHashA);
    await expect(
      registry.connect(other).registerRecord(fileHashA, metaHashA)
    ).to.be.revertedWith("Record already exists");
  });

  it("returns full record info", async function () {
    await registry.registerRecord(fileHashA, metaHashA);
    const info = await registry.getRecordInfo(1);
    expect(info[0]).to.equal(fileHashA);
    expect(info[1]).to.equal(metaHashA);
    expect(info[2]).to.equal(owner.address);
    expect(info[3]).to.be.gt(0);
  });

  it("auto-increments record id across distinct files", async function () {
    await registry.registerRecord(fileHashA, metaHashA);
    await registry.registerRecord(fileHashB, metaHashA);
    expect(await registry.getRecordByHash(fileHashA)).to.equal(1);
    expect(await registry.getRecordByHash(fileHashB)).to.equal(2);
    expect(await registry.totalRecords()).to.equal(2);
  });
});

