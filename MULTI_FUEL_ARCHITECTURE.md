# Kokonut Multi-Fuel Architecture

## Core Design

### 1. Genesis Block (Rules Only)
- No hardcoded Fuel Node list.
- Contains only network rules:
  - `minFuelStake`: 1,000,000 KNT (Minimum stake to become a Fuel Node)
  - `minFullStake`: 100 KNT (Minimum stake to become a Full Node)
  - `fuelConsensusThreshold`: 0.67 (2/3 consensus required)
  - `maxFuelNodes`: 100 (Maximum number of Fuel Nodes)
  - `faucetAmount`: 100 KNT (Faucet payout amount)

### 2. Fuel Node Registration (On-Chain)
- Fuel Nodes are registered directly on the blockchain.
- The first Fuel Node is the **Bootstrap Fuel** (registered in Block #1, immediately after Genesis).
- Subsequent Fuel Nodes are added through consensus among existing Fuel Nodes.

### 3. Fuel Node Discovery (Blockchain Scan)
- `BlockChain.getFuelNodes()`: Scans the blockchain to retrieve the current list of active Fuel Nodes.
- `BlockChain.scanFuelNodes()`: Scans the entire chain to track Fuel Node registrations and removals.

## Implemented Features

### New Data Types
- `NetworkRules`: Defines network parameters (included in Genesis).
- `FuelNodeInfo`: Contains Fuel Node metadata.
- `BlockDataType`: Enum for block types (TRANSACTION, FUEL_REGISTRATION, FUEL_REMOVAL, etc.).

### New API Endpoints
- `GET /getFuelNodes`: Retrieves the list of active Fuel Nodes.
- `GET /getNetworkRules`: Retrieves network rules from the Genesis Block.
- `POST /registerFuelNode`: Registers a new Fuel Node.

### Utilities
- `GenesisGenerator.createGenesisBlock()`: Utility to create the Genesis Block.
- `GenesisGenerator.createBootstrapFuelBlock()`: Utility to create the block for the first Fuel Node.

## Usage Examples

### Creating Genesis Block
```kotlin
val genesis = GenesisGenerator.createGenesisBlock(
    networkId = "kokonut-mainnet",
    timestamp = System.currentTimeMillis()
)
```

### Registering Bootstrap Fuel Node
```kotlin
val bootstrapFuel = GenesisGenerator.createBootstrapFuelBlock(
    fuelAddress = "http://fuel1.kokonut.io",
    fuelPublicKey = "MIIBIjAN...",
    stake = 1_000_000.0,
    previousHash = genesis.hash
)
```

### Retrieving Fuel Node List
```kotlin
val fuelNodes = BlockChain.getFuelNodes()
fuelNodes.forEach { fuel ->
    println("Fuel: ${fuel.address}, Stake: ${fuel.stake}")
}
```

### Dynamic Fuel Selection
```kotlin
// Select a random Fuel Node (Load Balancing)
val randomFuel = BlockChain.getRandomFuelNode()

// Select Primary (Bootstrap) Fuel Node
val primaryFuel = BlockChain.getPrimaryFuelNode()

// Usage
val fullNodes = randomFuel.getFullNodes()
val genesis = primaryFuel.getGenesisBlock()
```

### Registering a New Fuel Node
```kotlin
POST /registerFuelNode
Body: {
  "address": "http://fuel2.kokonut.io",
  "publicKey": "MIIBIjAN...",
  "stake": 1000000.0
}
```

## Advantages

✅ **Immutable Genesis**: Contains rules only, no mutable node lists.
✅ **Dynamic Scaling**: Fuel Nodes can be added anytime by writing to the blockchain.
✅ **Decentralization**: Supports multiple Fuel Nodes without hardcoding.
✅ **Transparency**: All node registrations are immutable records on the chain.
✅ **Scalability**: Supports up to 100 Fuel Nodes (configurable).
✅ **Load Balancing**: Random selection distributes traffic across nodes.
✅ **High Availability**: Automatic failover if the Primary Fuel Node goes down.

## Next Steps

1. Implement Faucet System
2. Implement Stake-based Full Node Registration
3. Implement Fuel Node Consensus Mechanism
4. Implement Fuel Node Reputation System
