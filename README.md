# Kokonut 🥥

**High-Performance Modular Blockchain Framework Powered by Kotlin with Proof of Stake**

[![DeepWiki](https://img.shields.io/badge/DeepWiki-Pascal--Institute%2Fkokonut-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/Pascal-Institute/kokonut)
[![](https://jitpack.io/v/Pascal-Institute/kokonut.svg)](https://jitpack.io/#Pascal-Institute/kokonut)
[![Docker FullNode](https://img.shields.io/docker/v/volta2030/knt_fullnode?sort=semver&label=knt_fullnode&logo=docker)](https://hub.docker.com/r/volta2030/knt_fullnode/tags)
[![Docker FuelNode](https://img.shields.io/docker/v/volta2030/knt_fuelnode?sort=semver&label=knt_fuelnode&logo=docker)](https://hub.docker.com/r/volta2030/knt_fuelnode/tags)

Kokonut is a lightweight and scalable blockchain framework written in Kotlin. Designed for educational and research purposes, it implements **Proof of Stake (PoS)** consensus with core blockchain principles (Staking, Validation, Wallets) using an intuitive multi-module architecture.

## 🌟 Key Features

- **⚡ Proof of Stake (PoS)**: Energy-efficient consensus mechanism - no wasteful mining
- **🔒 StakeVault Staking**: Stake KNT by locking funds to the on-chain stakeVault
- **📊 Stake-Weighted Selection**: Validators selected probabilistically based on stake amount
- **🌐 IPv6 P2P**: Direct peer-to-peer connectivity without NAT/port forwarding
- **🎯 Treasury-Paid Rewards (B-model)**: Validator rewards are recorded as treasury-paid on-chain transactions
- **🎁 Validator Onboarding Reward**: On first successful Light Node connect, the network records a 2 KNT onboarding payout (1 KNT to the Full Node reward receiver, 1 KNT to the validator)
- **🔐 Enhanced Chain Integrity**: Strict previousHash validation and timestamp consistency for blockchain security
- **💰 Smart Deposit Management**: Automatic deposit returns after staking completion with transaction logging

## 📚 Project Structure

This project is organized as a Gradle multi-module project, where each module plays a distinct role in the blockchain network.

### 1. `:library` (Core Framework)

Contains the core blockchain framework with a clean, minimal architecture:

```
library/src/main/kotlin/kokonut/
├── core/           # Core blockchain data structures
│   ├── Block.kt
│   ├── BlockChain.kt
│   ├── Data.kt
│   ├── FuelNodeInfo.kt
│   ├── NetworkRules.kt
│   ├── NodeHandshake.kt
│   ├── Policy.kt
│   ├── Transaction.kt
│   ├── Validator.kt
│   └── ValidatorOnboardingInfo.kt
├── state/          # State management
│   ├── NodeState.kt
│   └── ValidatorState.kt
└── util/           # Utilities and API
    ├── API.kt
    ├── FullNode.kt
    ├── GenesisGenerator.kt
    ├── NodeType.kt
    ├── Router.kt
    ├── SQLite.kt
    ├── Utility.kt
    ├── ValidatorPool.kt
    ├── ValidatorSession.kt
    └── Wallet.kt
```

#### Key Components:

- **`core/`**: Core blockchain entities (Block, BlockChain, Transaction, Validator)
- **`state/`**: Node and validator state enums
- **`util/`**: HTTP API, routing, utilities, and wallet management

### 2. `:fuelnode` (Bootstrap Node)

Acts as the network entry point and provides Node Discovery services.

- **Genesis Block**: Distributes the initial block (Genesis Block) to Full Nodes.
- **Node Discovery**: Manages and propagates the list of Full Nodes participating in the network.
- **Policy**: Manages network policies.

### 3. `:fullnode` (Main Node)

The core node that maintains and operates the actual blockchain network.

- **Validation**: Validates and creates new blocks using Proof of Stake (no energy-intensive mining).
- **Staking**: Validators stake by locking KNT to the stakeVault (on-chain).
- **Block Production**: Selected validators create blocks based on stake weight.
- **Propagation**: Propagates new blocks to other nodes.
- **API Server**: Provides an HTTP API for external monitoring of chain status.

### 4. `:lightnode` (Wallet Client)

A desktop wallet application for users (built with Compose Desktop).

- **Wallet Management**: Login via `.pem` key files.
- **Staking Operations**: Seamless staking workflow with automatic deposit management
  - Start Staking: Automatically calls `/stakeLock` if needed before validation
  - Stop Staking: Gracefully stops validation, syncs chain, and updates balance
- **Balance Updates**: Real-time balance updates after block validation and rewards
- **Transaction Recording**: Accurate transaction logging for all staking operations
- **Monitoring**: Check validating status and wallet address (Validator ID)

---

## 🚀 Getting Started

### Prerequisites

> **Requirement**: Kokonut requires **Java 17** or higher.

- **Java JDK 17** or higher
- **Kotlin 2.1.0** (Included in Gradle settings)

### Build

Run the following command in the project root to build the entire project.

```sh
# Windows
./gradlew.bat build

# Mac/Linux
./gradlew build
```

### How to Run

Each node can be run individually. The recommended order is **Fuel Node → Full Node (Multiple) → Light Node**.

#### 1. Run Fuel Node

```sh
./gradlew.bat :fuelnode:run
```

- The server starts on `http://[::]:80` (Dual Stack IPv4/IPv6) by default.
- **Note on IPv6**: When connecting via IPv6, enclose the address in brackets (e.g., `http://[2001:db8::1]:80`).
- **IPv6 Priority**: This project prioritizes IPv6 for direct peer-to-peer connectivity without NAT. While the server binds to `::` (dual stack), IPv4-only clients may experience limitations in P2P scenarios.

#### 2. Run Full Node

```sh
./gradlew.bat :fullnode:run
```

Full Nodes **must** join an existing network:

- Set `KOKONUT_PEER` to a reachable Fuel Node or peer Full Node (e.g., `http://<FUEL_NODE_IP>:80`).

Optional:

- Set `KOKONUT_FULLNODE_REWARD_RECEIVER` to control where the onboarding reward is credited for the Full Node (defaults to the Full Node's own URL as seen by the server).
- Set `KOKONUT_ADVERTISE_ADDRESS` to the publicly reachable base URL for this Full Node (used as the source address for propagation so other Full Nodes can pull your chain correctly).
- Set `KOKONUT_TREASURY_ADDRESS` to override the treasury address (default: `KOKONUT_TREASURY`).
- Set `KOKONUT_STAKE_VAULT_ADDRESS` to override the stake vault address (default: `KOKONUT_STAKE_VAULT`).

#### 3. Run Light Node (Wallet)

```sh
./gradlew.bat :lightnode:run
```

- A GUI window will appear. Load your `.pem` key files to log in.

---

## 📡 API Reference

Full Nodes and the Fuel Node interact via HTTP APIs.

### Full Node API

| Method | Endpoint                  | Description                                                    |
| :----: | :------------------------ | :------------------------------------------------------------- |
| `GET`  | `/`                       | Node status and information dashboard (HTML)                   |
| `POST` | `/handshake`              | Client handshake (returns network info)                        |
| `GET`  | `/getLastBlock`           | Get the last block of the current chain                        |
| `GET`  | `/getChain`               | Get the entire blockchain data                                 |
| `GET`  | `/getBalance?address=`    | Get wallet balance for a given address (returns JSON)          |
| `GET`  | `/isValid`                | Check the integrity of the local blockchain                    |
| `GET`  | `/getTotalCurrencyVolume` | Get the total supply of KNT                                    |
| `GET`  | `/getReward`              | Get the current reward policy value                            |
| `GET`  | `/getValidators`          | Get the current validator set                                  |
| `POST` | `/stakeLock`              | Lock stake to stakeVault (creates on-chain `STAKE_LOCK` block) |
| `POST` | `/startValidating`        | Register a validator session (requires stake >= minimum)       |
| `POST` | `/stopValidating`         | Stop validating session                                        |
| `POST` | `/addBlock`               | Submit a validated block (network propagation)                 |

### Fuel Node API

| Method | Endpoint           | Description                                  |
| :----: | :----------------- | :------------------------------------------- |
| `GET`  | `/getGenesisBlock` | Download the Genesis Block data              |
| `POST` | `/heartbeat`       | Full Node heartbeat registration (discovery) |
| `GET`  | `/getFullNodes`    | Get the list of active Full Nodes            |
| `GET`  | `/getPolicy`       | Get protocol/version policy                  |
| `GET`  | `/getChain`        | Get the blockchain data                      |

---

## 🎁 Validator Onboarding Reward (1-time)

On the first successful Light Node connect (`POST /handshake` from a LIGHT client), the Full Node appends a `VALIDATOR_ONBOARDING` block to the chain and records two onboarding transactions:

- 1 KNT to the Full Node reward receiver (`KOKONUT_FULLNODE_REWARD_RECEIVER`)
- 1 KNT to the validator address

These payouts are funded by the treasury (`KOKONUT_TREASURY_ADDRESS`).

**Persistence**: This onboarding event is stored in `kovault.db` (SQLite). If you keep `kovault.db` via Docker volume mounts, the 1-time rule survives container rebuilds.

---

## 🔒 Staking (stakeVault lock)

To become an active validator, you must lock at least the network minimum stake (currently **1 KNT**) to the stakeVault address.

- Full Nodes expose `POST /stakeLock`, which appends a `STAKE_LOCK` block containing an on-chain transfer from your validator address to the stakeVault.
- Light Node "Start Staking" will automatically call `POST /stakeLock` (if needed) before starting validation.
- **Stop Staking**: When you stop validation, a `POST /stopValidating` call creates an `UNSTAKE` block that returns your deposit from stakeVault to your validator address.

**Chain Integrity**: All blocks maintain strict timestamp consistency between blocks and their embedded transactions, ensuring hash validation integrity across the network.

**Note**: In the current model, validator stake and rewards are derived from the blockchain history (the chain is the source of truth).

---

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) 2.1.0
- **Server Framework**: [Ktor](https://ktor.io/) 3.0.2 (Netty Engine)
- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) 1.7.1 (Desktop)
- **Serialization**: Kotlinx Serialization (JSON)
- **Database**: SQLite 3.47.1.0 (via JDBC)
- **Logging**: Logback 1.5.12
- **Build Tool**: Gradle Kotlin DSL

## 🤝 Contributing

1. Fork this repository.
2. Create a new branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add some amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

### Development Guidelines

- **Core entities**: Add to `core/` package
- **Utilities and API**: Add to `util/` package
- **State management**: Add to `state/` package
- **Follow Kotlin conventions** and maintain backward compatibility
- **Write tests** for new features
- **Timestamp Consistency**: When creating transactions within blocks, always use the block's timestamp to ensure hash integrity

## 🔧 Recent Updates (January 2026)

### Version Upgrades

- **Kotlin**: Upgraded to 2.1.0 from 1.9.22
- **Ktor**: Upgraded to 3.0.2 from 2.3.4 with API compatibility fixes
- **Compose Desktop**: Upgraded to 1.7.1 from 1.6.0
- **SQLite JDBC**: Updated to 3.47.1.0
- **Logback**: Updated to 1.5.12

### Blockchain Improvements

- **Enhanced Chain Integrity**: Implemented strict `previousHash` validation and exact index verification in block addition
- **Timestamp Consistency Fix**: All transactions now use their containing block's timestamp, ensuring hash recalculation consistency
- **Improved Validation Logging**: Enhanced error reporting shows specific invalid blocks with hash comparisons

### Light Node Enhancements

- **Stop Staking Workflow**: Properly calls `/stopValidating` API, syncs chain, and updates balance
- **Post-Validation Updates**: Chain sync and balance updates after successful block validation
- **Transaction Logging**: Accurate recording of all reward transactions

### Governance & Security (New)

- **⚖️ Deterministic Consensus**: Validator selection is now fully deterministic.
  - Resolves network forks by ensuring all nodes select the exact same validator for every block height using a consensus seed (`previousHash + index`).
  - Eliminates "lucky" or "loud" block production; only the legitimately selected validator's block is accepted.
- **🛡️ Fuel Node Slashing Authority**:
  - Implemented a **Fuel Node-exclusive Slashing mechanism**.
  - Only the Fuel Node ("Judiciary") has the authority to issue `SLASH` transactions to penalize malicious validators.
  - Protects the network integrity by centralizing the "punishment" power to the bootstrap node.

---

**License**
This project is licensed under the MIT License. See the `LICENSE` file for details.

## 🐳 Docker Hub & Usage

Official Docker images are available on Docker Hub.

- **Fuel Node**: `volta2030/knt_fuelnode`
- **Full Node**: `volta2030/knt_fullnode`

### Running with Docker

To ensure data persistence (blockchain data, keys), you **must mount a volume** to `/data`.

These images set `KOKONUT_DATA_DIR=/data`, so `kovault.db` will be created under the mounted `/data` directory.

#### 1. Run Fuel Node

The Fuel Node acts as the network bootstrapper.

```bash
# -p 80:80 : Bind container port 80 to host port 80
# -v ./data:/data : Persist blockchain data to host's ./data directory
docker run -d \
  --name knt_fuelnode \
  -p 80:80 \
  -v $(pwd)/data_fuel:/data \
  volta2030/knt_fuelnode
```

#### 2. Run Full Node

Full Nodes connect to the network. You can specify a peer to connect to using `KOKONUT_PEER`.

```bash
# -e KOKONUT_PEER : Address of a known node (e.g., Fuel Node or another Full Node)
# -v ./data_full:/data : Use a separate data directory for the Full Node
docker run -d \
  --name knt_fullnode \
  -p 8080:80 \
  -e KOKONUT_PEER="http://<FUEL_NODE_IP>:80" \
  -v $(pwd)/data_full:/data \
  volta2030/knt_fullnode
```

> **Critical Note**: Full Nodes **MUST** configure `KOKONUT_PEER` to join an existing network.
> If `KOKONUT_PEER` is omitted or the target peer is unreachable, the Full Node will **fail to start** (throw an exception).
> This mechanism prevents accidental network splitting (Split Brain). Only the Fuel Node is allowed to start without a peer to generate the Genesis Block.
>
> **Note**: Replace `<FUEL_NODE_IP>` with the actual IP address or domain of your Fuel Node. If running on the same machine, use the host's local network IP.
