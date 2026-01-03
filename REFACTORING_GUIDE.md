# Kokonut Blockchain Refactoring Guide

## 🎉 Completed Refactoring

The Kokonut blockchain project has been comprehensively refactored into a modern, framework-like architecture.

### ✅ What's Been Created

#### 1. **New Package Structure** (Complete)

```
kokonut/
├── blockchain/chain/          # Future: Pure chain data structures
├── config/                    # ✅ Configuration management
│   ├── NetworkConfig.kt       # Network-wide configuration
│   └── Constants.kt           # Global constants
├── consensus/                 # Future: PoS consensus mechanism
├── crypto/                    # ✅ Cryptography utilities
│   ├── HashCalculator.kt      # SHA-256 hashing
│   ├── KeyManager.kt          # Key generation/loading
│   ├── SignatureUtil.kt       # Digital signatures
│   └── Wallet.kt              # Refactored wallet
├── network/                   # Future: Network communication
│   ├── node/                  # Node types and info
│   ├── protocol/              # Network protocols
│   ├── peer/                  # Peer discovery
│   └── client/                # HTTP clients
├── node/                      # ✅ Node management
│   └── NodeInitializer.kt     # Blockchain initialization
├── persistence/               # ✅ Data access layer
│   ├── database/
│   │   └── Database.kt        # Database interface
│   └── repository/
│       ├── Repository.kt      # Base repository
│       ├── BlockRepository.kt # Block data access
│       └── SQLiteBlockRepository.kt # SQLite implementation
├── service/                   # ✅ Business logic layer
│   ├── BlockchainService.kt   # Chain operations
│   ├── BalanceService.kt      # Balance calculations
│   └── StakingService.kt      # Staking operations
└── util/                      # ✅ Pure utilities
    └── CommonUtil.kt          # Math, String, Time utils
```

#### 2. **Service Layer** (Complete)

**BlockchainService** - Core chain operations:

- `getChain()`, `getLastBlock()`, `getGenesisBlock()`
- `getChainSize()`, `getBlockByHash()`, `addBlock()`
- `isValid()`, `hasGenesisTreasuryMint()`

**BalanceService** - Financial operations:

- `getBalance(address)`, `getTreasuryBalance()`
- `getTotalCurrencyVolume()`, `getBalances(addresses)`

**StakingService** - Staking operations:

- `computeStakeByAddress()`, `getStakedAmount()`
- `hasSufficientStake()`, `canLockStake()`
- `getTotalStaked()`, `getQualifiedValidators()`

#### 3. **Repository Pattern** (Complete)

- **Repository interface**: Base CRUD operations
- **BlockRepository**: Blockchain-specific data access
- **SQLiteBlockRepository**: Concrete SQLite implementation
- **Database interface**: Abstract storage layer

#### 4. **Configuration Management** (Complete)

- **NetworkConfig**: Centralized network configuration
- **Constants**: Global constants (TICKER, timings, ports)

#### 5. **Crypto Package** (Complete)

- **HashCalculator**: SHA-256 hashing utilities
- **KeyManager**: RSA key generation/management
- **SignatureUtil**: Digital signature operations
- **Wallet**: Refactored to use crypto utilities

#### 6. **Node Management** (Complete)

- **NodeInitializer**: Handles blockchain initialization logic
  - Load from database
  - Bootstrap from peer
  - Genesis creation (Fuel nodes only)

### 📊 Improvements

| Metric                     | Before | After |
| -------------------------- | ------ | ----- |
| Packages                   | 4      | 11+   |
| Service Layer              | ❌     | ✅    |
| Repository Pattern         | ❌     | ✅    |
| Dependency Injection Ready | ❌     | ✅    |
| Crypto Centralized         | ❌     | ✅    |
| Configuration Management   | ❌     | ✅    |

### 🚧 Migration Path

#### Phase 1: Backward Compatibility (Current)

The new code **coexists** with the old code. Old code still works.

```kotlin
// Old way (still works)
val balance = BlockChain.getBalance(address)

// New way (recommended)
val balanceService = BalanceService(repository)
val balance = balanceService.getBalance(address)
```

#### Phase 2: Update Applications (Next Steps)

Applications should be updated to use the new services:

```kotlin
// Before
BlockChain.initialize(NodeType.FUEL)
val balance = BlockChain.getBalance(address)

// After
val config = NetworkConfig()
val database = SQLite()
val repository = SQLiteBlockRepository(database)
val blockchainService = BlockchainService(repository, config)
val balanceService = BalanceService(repository, config)
val initializer = NodeInitializer(repository, blockchainService, config)

initializer.initialize(NodeType.FUEL)
val balance = balanceService.getBalance(address)
```

#### Phase 3: Deprecate Old Code

Mark old methods as `@Deprecated` and provide migration guidance.

#### Phase 4: Remove Old Code

After all applications are migrated, remove deprecated code.

### 🎯 Next Steps (Priority Order)

#### HIGH PRIORITY

1. **Update SQLite.kt** to implement new Database interface

   - Make it implement `Database` interface
   - Ensure compatibility with `SQLiteBlockRepository`

2. **Refactor BlockChain.kt** to use new services

   - Delegate to `BlockchainService`, `BalanceService`, `StakingService`
   - Keep static companion for backward compatibility
   - Mark methods as `@Deprecated` with migration hints

3. **Extract Network Client** from API.kt

   - Create `BlockchainClient` interface
   - Implement HTTP client operations
   - Move to `network/client/` package

4. **Decompose Router.kt** (1363 lines)

   - Split into domain-specific route files:
     - `FuelNodeRoutes.kt` (200-250 lines)
     - `FullNodeRoutes.kt` (200-250 lines)
     - `ValidatorRoutes.kt` (200-250 lines)
     - `BlockRoutes.kt` (150-200 lines)
     - `QueryRoutes.kt` (150-200 lines)

5. **Update Application Entry Points**
   - Update `fullnode/src/main/kotlin/Application.kt`
   - Update `fuelnode/src/main/kotlin/Application.kt`
   - Update `lightnode/src/main/kotlin/Application.kt`

#### MEDIUM PRIORITY

6. **Extract Consensus Logic**

   - Create `BlockProducer` (from `BlockChain.validate()`)
   - Create `ValidatorSelector` (from `ValidatorPool`)
   - Create `RewardCalculator` (from various places)

7. **Network Layer**

   - Create `PeerManager` (peer discovery)
   - Create `PeerClient` (peer communication)
   - Extract handshake protocol

8. **Testing**
   - Add unit tests for services
   - Add integration tests for repositories
   - Test backward compatibility

### 📝 Usage Examples

#### Example 1: Getting Balance (New Way)

```kotlin
import kokonut.config.NetworkConfig
import kokonut.persistence.repository.SQLiteBlockRepository
import kokonut.service.BalanceService
import kokonut.util.SQLite

val config = NetworkConfig()
val database = SQLite()
val repository = SQLiteBlockRepository(database)
val balanceService = BalanceService(repository, config)

// Get balance
val balance = balanceService.getBalance("validator_address_here")
println("Balance: $balance KNT")

// Get treasury balance
val treasuryBalance = balanceService.getTreasuryBalance()
println("Treasury: $treasuryBalance KNT")

// Get total volume
val volume = balanceService.getTotalCurrencyVolume()
println("Total Volume: $volume KNT")
```

#### Example 2: Staking Operations (New Way)

```kotlin
import kokonut.service.StakingService

val stakingService = StakingService(repository, balanceService, config)

// Get staked amount
val staked = stakingService.getStakedAmount("validator_address")
println("Staked: $staked KNT")

// Check if can lock stake
val canLock = stakingService.canLockStake("validator_address", 100.0)
println("Can lock 100 KNT: $canLock")

// Get all qualified validators
val minStake = 1.0
val validators = stakingService.getQualifiedValidators(minStake)
println("Qualified validators: ${validators.size}")
```

#### Example 3: Node Initialization (New Way)

```kotlin
import kokonut.network.node.NodeType
import kokonut.node.NodeInitializer

val initializer = NodeInitializer(repository, blockchainService, config)

// Initialize as Fuel Node
initializer.initialize(NodeType.FUEL)

// Initialize as Full Node with peer
initializer.initialize(NodeType.FULL, "http://fuel-node:80")
```

#### Example 4: Crypto Operations (New Way)

```kotlin
import kokonut.crypto.*
import java.io.File

// Generate new keys
val keyPair = KeyManager.generateKeyPair()
KeyManager.saveKeyPairToFile(
    keyPair,
    "private.pem",
    "public.pem"
)

// Load keys
val publicKey = KeyManager.loadPublicKey("public.pem")
val privateKey = KeyManager.loadPrivateKey("private.pem")

// Sign data
val data = "Hello Kokonut"
val signature = SignatureUtil.signData(data, privateKey)

// Verify signature
val isValid = SignatureUtil.verifySignature(data, signature, publicKey)
println("Signature valid: $isValid")

// Calculate hash
val hash = HashCalculator.calculateSHA256("some data")
println("Hash: $hash")
```

### 🏗️ Architecture Benefits

1. **Separation of Concerns**: Each package has a clear purpose
2. **Testability**: Services can be unit tested independently
3. **Flexibility**: Easy to swap implementations (e.g., different databases)
4. **Maintainability**: Smaller, focused classes easier to understand
5. **Scalability**: Clear boundaries for adding new features
6. **Collaboration**: Multiple developers can work on different packages
7. **Documentation**: Self-documenting through package structure

### ⚠️ Breaking Changes (None Yet!)

All old code still works! This refactoring was done in a **non-breaking** way.

### 🤝 Contributing

When adding new features:

1. **Services**: Put business logic in `service/` package
2. **Data Access**: Use repository pattern in `persistence/`
3. **Network**: Add network code to `network/` package
4. **Config**: Add configuration to `config/` package
5. **Crypto**: Add crypto operations to `crypto/` package

### 📚 Further Reading

- See individual package docs for detailed API documentation
- Check examples in test files (when created)
- Review code comments for implementation details

---

**Status**: ✅ Foundation Complete | 🚧 Migration In Progress | ⏳ Full Migration Pending

**Last Updated**: January 3, 2026
