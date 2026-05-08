const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("AccessControlManager", function () {
  let registry;
  let acm;
  let alice; // record owner
  let bob;   // grantee
  let carol; // 3rd party
  let recordId;

  beforeEach(async function () {
    [alice, bob, carol] = await ethers.getSigners();

    const RecordRegistry = await ethers.getContractFactory("RecordRegistry");
    registry = await RecordRegistry.deploy();
    await registry.waitForDeployment();

    const AccessControlManager = await ethers.getContractFactory("AccessControlManager");
    acm = await AccessControlManager.deploy(await registry.getAddress());
    await acm.waitForDeployment();

    // Alice 上传一个成果
    const tx = await registry.connect(alice).registerRecord(ethers.id("doc-1"), ethers.id("meta-1"));
    await tx.wait();
    recordId = 1;
  });

  it("only record owner can grant access", async function () {
    await expect(
      acm.connect(bob).grantAccess(recordId, carol.address, "READ", 0)
    ).to.be.revertedWith("Only record owner can operate");
  });

  it("grant + check returns true", async function () {
    await acm.connect(alice).grantAccess(recordId, bob.address, "READ", 0);
    expect(await acm.checkAccess(recordId, bob.address)).to.be.true;
    expect(await acm.checkAccess(recordId, carol.address)).to.be.false;
  });

  it("rejects zero-address grantee", async function () {
    await expect(
      acm.connect(alice).grantAccess(recordId, ethers.ZeroAddress, "READ", 0)
    ).to.be.revertedWith("Invalid grantee address");
  });

  it("rejects self-grant", async function () {
    await expect(
      acm.connect(alice).grantAccess(recordId, alice.address, "READ", 0)
    ).to.be.revertedWith("Cannot grant to yourself");
  });

  it("rejects past expireTime", async function () {
    const past = (await ethers.provider.getBlock("latest")).timestamp - 100;
    await expect(
      acm.connect(alice).grantAccess(recordId, bob.address, "READ", past)
    ).to.be.revertedWith("expireTime must be in the future or zero");
  });

  it("expired authorization fails check", async function () {
    const future = (await ethers.provider.getBlock("latest")).timestamp + 5;
    await acm.connect(alice).grantAccess(recordId, bob.address, "READ", future);
    expect(await acm.checkAccess(recordId, bob.address)).to.be.true;

    // 推进区块时间
    await ethers.provider.send("evm_increaseTime", [10]);
    await ethers.provider.send("evm_mine", []);
    expect(await acm.checkAccess(recordId, bob.address)).to.be.false;
  });

  it("revoke disables access and emits event", async function () {
    await acm.connect(alice).grantAccess(recordId, bob.address, "READ", 0);
    await expect(acm.connect(alice).revokeAccess(recordId, bob.address))
      .to.emit(acm, "AccessRevoked")
      .withArgs(recordId, alice.address, bob.address);
    expect(await acm.checkAccess(recordId, bob.address)).to.be.false;
  });

  it("revoke without prior grant reverts", async function () {
    await expect(
      acm.connect(alice).revokeAccess(recordId, bob.address)
    ).to.be.revertedWith("Access not granted");
  });

  it("getAuthorization returns full struct", async function () {
    await acm.connect(alice).grantAccess(recordId, bob.address, "DOWNLOAD", 0);
    const info = await acm.getAuthorization(recordId, bob.address);
    expect(info[0]).to.equal(alice.address);
    expect(info[1]).to.equal("DOWNLOAD");
    expect(info[2]).to.equal(0n);
    expect(info[3]).to.be.true;
  });
});
