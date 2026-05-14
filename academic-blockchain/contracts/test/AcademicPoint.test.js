const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("AcademicPoint (ERC20 incentive)", function () {
  let point;
  let owner, minter, alice;

  beforeEach(async function () {
    [owner, minter, alice] = await ethers.getSigners();
    const AcademicPoint = await ethers.getContractFactory("AcademicPoint");
    point = await AcademicPoint.deploy();
    await point.waitForDeployment();
  });

  it("has name and symbol", async function () {
    expect(await point.name()).to.equal("Academic Point");
    expect(await point.symbol()).to.equal("ACP");
    expect(await point.decimals()).to.equal(18);
  });

  it("non-minter cannot reward", async function () {
    await expect(
      point.connect(alice).reward(alice.address, 1, "test")
    ).to.be.revertedWith("Caller is not a minter");
  });

  it("owner can add/remove minter", async function () {
    await expect(point.addMinter(minter.address))
      .to.emit(point, "MinterAdded").withArgs(minter.address);
    expect(await point.minters(minter.address)).to.be.true;

    await point.removeMinter(minter.address);
    expect(await point.minters(minter.address)).to.be.false;
  });

  it("only owner can manage minters", async function () {
    await expect(point.connect(alice).addMinter(alice.address))
      .to.be.revertedWith("Ownable: caller is not the owner");
  });

  it("minter can mint with reason", async function () {
    await point.addMinter(minter.address);
    const amount = ethers.parseUnits("5", 18);
    await expect(point.connect(minter).reward(alice.address, amount, "share-reward"))
      .to.emit(point, "Rewarded").withArgs(alice.address, amount, "share-reward");
    expect(await point.balanceOf(alice.address)).to.equal(amount);
  });
});
