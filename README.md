# Kokonut 🥥

**High-Performance Modular Blockchain Framework Powered by Kotlin with Proof of Stake**

Kokonut is a lightweight and scalable blockchain framework written in Kotlin. Designed for educational and research purposes, it implements **Proof of Stake (PoS)** consensus with core blockchain principles (Staking, Validation, Wallets) using an intuitive multi-module architecture.

## 🌟 Key Features

- **⚡ Proof of Stake (PoS)**: Energy-efficient consensus mechanism - no wasteful mining
- **🔒 Validator Staking**: Stake KNT tokens to participate in block validation
- **📊 Stake-Weighted Selection**: Validators selected probabilistically based on stake amount
- **🌐 IPv6 P2P**: Direct peer-to-peer connectivity without NAT/port forwarding
- **🎯 Minimal Rewards**: Transaction fee-based rewards (1% to validators)
- **🎁 Validator Onboarding Reward**: On first validator registration, the network records a 2 KNT onboarding payout (1 KNT to the Full Node reward receiver, 1 KNT to the validator)

## 📚 Project Structure

This project is organized as a Gradle multi-module project, where each module plays a distinct role in the blockchain network.

### 1. `:library` (Core)

Contains the core logic and data structures of the blockchain.

- **Block & Blockchain**: Block creation, hash calculation (SHA-256), chain linking, and validation logic.
- **Validator & ValidatorPool**: Proof of Stake validator management and stake-weighted selection.
- **Wallet**: Public/Private key generation and verification (Digital Signatures).
- **Router**: API definitions for node-to-node communication.
- **PoS (Proof of Stake)**: Energy-efficient stake-based consensus algorithm.

### 2. `:fuelnode` (Bootstrap Node)

Acts as the network entry point and provides Node Discovery services.

- **Genesis Block**: Distributes the initial block (Genesis Block) to Full Nodes.
- **Node Discovery**: Manages and propagates the list of Full Nodes participating in the network.
- **Policy**: Manages protocol versions and network policies.

### 3. `:fullnode` (Main Node)

The core node that maintains and operates the actual blockchain network.

- **Validation**: Validates and creates new blocks using Proof of Stake (no energy-intensive mining).
- **Staking**: Nodes stake KNT to become validators.
- **Block Production**: Selected validators create blocks based on stake weight.
- **Propagation**: Propagates new blocks to other nodes.
- **API Server**: Provides an HTTP API for external monitoring of chain status.

### 4. `:lightnode` (Wallet Client)

A desktop wallet application for users (built with Compose Desktop).

- **Wallet Management**: Login via `.pem` key files.
- **Monitoring**: Check mining status and wallet address (Miner ID).

---

## 🚀 Getting Started

### Prerequisites

> **Requirement**: Kokonut requires **Java 17** or higher.

- **Java JDK 17** or higher
- **Kotlin 1.9.22** (Included in Gradle settings)

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

#### 3. Run Light Node (Wallet)

```sh
./gradlew.bat :lightnode:run
```

- A GUI window will appear. Load your `.pem` key files to log in.

---

## 📡 API Reference

Full Nodes and the Fuel Node interact via HTTP APIs.

### Full Node API

| Method | Endpoint                  | Description                                                      |
| :----: | :------------------------ | :--------------------------------------------------------------- |
| `GET`  | `/`                       | Node status and information dashboard (HTML)                     |
| `POST` | `/handshake`              | Client handshake (returns network info)                          |
| `GET`  | `/getLastBlock`           | Get the last block of the current chain                          |
| `GET`  | `/getChain`               | Get the entire blockchain data                                   |
| `GET`  | `/isValid`                | Check the integrity of the local blockchain                      |
| `GET`  | `/getTotalCurrencyVolume` | Get the total supply of KNT                                      |
| `GET`  | `/getReward`              | Get the current reward policy value                              |
| `GET`  | `/getValidators`          | Get the current validator set                                    |
| `POST` | `/startValidating`        | Register a validator session (triggers 1-time onboarding reward) |
| `POST` | `/stopValidating`         | Stop validating session                                          |
| `POST` | `/addBlock`               | Submit a validated block (network propagation)                   |

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

When a validator registers for the first time via `POST /startValidating`, the Full Node appends a `VALIDATOR_ONBOARDING` block to the chain and records two onboarding transactions:

- 1 KNT to the Full Node reward receiver (`KOKONUT_FULLNODE_REWARD_RECEIVER`)
- 1 KNT to the validator address

The "2 KNT withdrawn from Fuel Node" is represented as the sender field in those transactions using the primary Fuel Node address.

**Persistence**: This onboarding event is stored in `kovault.db` (SQLite). If you keep `kovault.db` via Docker volume mounts, the 1-time rule survives container rebuilds.

---

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **Server Framework**: [Ktor](https://ktor.io/) (Netty Engine)
- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Desktop)
- **Serialization**: Kotlinx Serialization (JSON)
- **Build Tool**: Gradle Kotlin DSL

## 🤝 Contributing

1. Fork this repository.
2. Create a new branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add some amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

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
