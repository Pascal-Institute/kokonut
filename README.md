# Kokonut 🥥
**High-Performance Modular Blockchain Framework Powered by Kotlin with Proof of Stake**

Kokonut is a lightweight and scalable blockchain framework written in Kotlin. Designed for educational and research purposes, it implements **Proof of Stake (PoS)** consensus with core blockchain principles (Staking, Validation, Wallets) using an intuitive multi-module architecture.

## 🌟 Key Features

- **⚡ Proof of Stake (PoS)**: Energy-efficient consensus mechanism - no wasteful mining
- **🔒 Validator Staking**: Stake KNT tokens to participate in block validation
- **📊 Stake-Weighted Selection**: Validators selected probabilistically based on stake amount
- **🌐 IPv6 P2P**: Direct peer-to-peer connectivity without NAT/port forwarding
- **🎯 Minimal Rewards**: Transaction fee-based rewards (1% to validators)

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

#### 3. Run Light Node (Wallet)
```sh
./gradlew.bat :lightnode:run
```
- A GUI window will appear. Load your `.pem` key files to log in.

---

## 📡 API Reference

Full Nodes and the Fuel Node interact via HTTP APIs.

### Full Node API
| Method | Endpoint | Description |
|:---:|:---|:---|
| `GET` | `/` | Node status and information dashboard (HTML) |
| `GET` | `/getLastBlock` | Get the last block of the current chain |
| `GET` | `/getChain` | Get the entire blockchain data |
| `GET` | `/isValid` | Check the integrity of the local blockchain |
| `GET` | `/getTotalCurrencyVolume` | Get the total supply of KNT |
| `POST` | `/startMining` | Start mining (Miner key required) |
| `POST` | `/addBlock` | Submit a mined block (Network propagation) |

### Fuel Node API
| Method | Endpoint | Description |
|:---:|:---|:---|
| `POST` | `/submit` | Register to participate in the Full Node network |
| `GET` | `/getGenesisBlock` | Download the Genesis Block data |
| `GET` | `/getFullNodes` | Get the list of active Full Nodes |

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
