const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying contracts with account:", deployer.address);
  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("Account balance:", hre.ethers.formatEther(balance), "ETH");

  // 1. RecordRegistry (ERC721)
  const RecordRegistry = await hre.ethers.getContractFactory("RecordRegistry");
  const registry = await RecordRegistry.deploy();
  await registry.waitForDeployment();
  const registryAddress = await registry.getAddress();
  console.log("RecordRegistry (ERC721 NFT) deployed at:", registryAddress);

  // 2. AcademicPoint (ERC20 incentive)
  const AcademicPoint = await hre.ethers.getContractFactory("AcademicPoint");
  const point = await AcademicPoint.deploy();
  await point.waitForDeployment();
  const pointAddress = await point.getAddress();
  console.log("AcademicPoint (ERC20) deployed at:", pointAddress);

  // 3. AccessControlManager (依赖 RecordRegistry + AcademicPoint)
  const rewardPerGrant = hre.ethers.parseUnits("1", 18); // 每次授权奖励 1 ACP
  const AccessControlManager = await hre.ethers.getContractFactory("AccessControlManager");
  const acm = await AccessControlManager.deploy(registryAddress, pointAddress, rewardPerGrant);
  await acm.waitForDeployment();
  const acmAddress = await acm.getAddress();
  console.log("AccessControlManager deployed at:", acmAddress);

  // 4. 授予 AccessControlManager 在 AcademicPoint 中 mint 的权限
  const addMinterTx = await point.addMinter(acmAddress);
  await addMinterTx.wait();
  console.log("Added AccessControlManager as minter on AcademicPoint, tx:", addMinterTx.hash);

  // 5. 输出地址 & ABI 给后端 / 前端使用
  const networkName = hre.network.name;
  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });

  const summary = {
    network: networkName,
    chainId: Number((await hre.ethers.provider.getNetwork()).chainId),
    deployer: deployer.address,
    deployedAt: new Date().toISOString(),
    rewardPerGrant: rewardPerGrant.toString(),
    contracts: {
      RecordRegistry: registryAddress,
      AcademicPoint: pointAddress,
      AccessControlManager: acmAddress,
    },
  };
  fs.writeFileSync(
    path.join(outDir, `${networkName}.json`),
    JSON.stringify(summary, null, 2)
  );
  console.log("\nDeployment summary written to:", path.join(outDir, `${networkName}.json`));

  // 6. 把 ABI 拷贝到前后端共享目录
  const sharedAbiDir = path.join(__dirname, "..", "..", "frontend", "src", "abi");
  fs.mkdirSync(sharedAbiDir, { recursive: true });
  for (const name of ["RecordRegistry", "AccessControlManager", "AcademicPoint"]) {
    const artifactPath = path.join(__dirname, "..", "artifacts", "contracts", `${name}.sol`, `${name}.json`);
    const artifact = JSON.parse(fs.readFileSync(artifactPath, "utf-8"));
    fs.writeFileSync(
      path.join(sharedAbiDir, `${name}.json`),
      JSON.stringify({ abi: artifact.abi, bytecode: artifact.bytecode }, null, 2)
    );
  }

  // 7. 写一份 addresses.json 给前端使用
  fs.writeFileSync(
    path.join(sharedAbiDir, "addresses.json"),
    JSON.stringify(
      {
        chainId: summary.chainId,
        RecordRegistry: registryAddress,
        AcademicPoint: pointAddress,
        AccessControlManager: acmAddress,
      },
      null,
      2
    )
  );
  console.log("ABI + addresses synced to frontend/src/abi/");

  console.log("\n=== Next step ===");
  console.log("Set these in backend/src/main/resources/application.yml (or as env vars):");
  console.log(`  blockchain.contracts.record-registry: "${registryAddress}"`);
  console.log(`  blockchain.contracts.access-control-manager: "${acmAddress}"`);
  console.log(`  blockchain.contracts.academic-point: "${pointAddress}"`);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
