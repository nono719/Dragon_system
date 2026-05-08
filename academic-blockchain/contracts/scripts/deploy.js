const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying contracts with account:", deployer.address);
  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("Account balance:", hre.ethers.formatEther(balance), "ETH");

  // 1. RecordRegistry
  const RecordRegistry = await hre.ethers.getContractFactory("RecordRegistry");
  const registry = await RecordRegistry.deploy();
  await registry.waitForDeployment();
  const registryAddress = await registry.getAddress();
  console.log("RecordRegistry deployed at:", registryAddress);

  // 2. AccessControlManager（依赖 RecordRegistry 地址）
  const AccessControlManager = await hre.ethers.getContractFactory("AccessControlManager");
  const acm = await AccessControlManager.deploy(registryAddress);
  await acm.waitForDeployment();
  const acmAddress = await acm.getAddress();
  console.log("AccessControlManager deployed at:", acmAddress);

  // 3. 输出地址 & ABI 给后端 / 前端使用
  const networkName = hre.network.name;
  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });

  const summary = {
    network: networkName,
    chainId: Number((await hre.ethers.provider.getNetwork()).chainId),
    deployer: deployer.address,
    deployedAt: new Date().toISOString(),
    contracts: {
      RecordRegistry: registryAddress,
      AccessControlManager: acmAddress,
    },
  };
  fs.writeFileSync(
    path.join(outDir, `${networkName}.json`),
    JSON.stringify(summary, null, 2)
  );
  console.log("\nDeployment summary written to:", path.join(outDir, `${networkName}.json`));

  // 4. 把 ABI 拷贝到前后端共享目录
  const sharedAbiDir = path.join(__dirname, "..", "..", "frontend", "src", "abi");
  fs.mkdirSync(sharedAbiDir, { recursive: true });
  for (const name of ["RecordRegistry", "AccessControlManager"]) {
    const artifactPath = path.join(__dirname, "..", "artifacts", "contracts", `${name}.sol`, `${name}.json`);
    const artifact = JSON.parse(fs.readFileSync(artifactPath, "utf-8"));
    fs.writeFileSync(
      path.join(sharedAbiDir, `${name}.json`),
      JSON.stringify({ abi: artifact.abi, bytecode: artifact.bytecode }, null, 2)
    );
  }

  // 5. 写一份 addresses.json 给前端使用
  fs.writeFileSync(
    path.join(sharedAbiDir, "addresses.json"),
    JSON.stringify(
      {
        chainId: summary.chainId,
        RecordRegistry: registryAddress,
        AccessControlManager: acmAddress,
      },
      null,
      2
    )
  );
  console.log("ABI + addresses synced to frontend/src/abi/");
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
