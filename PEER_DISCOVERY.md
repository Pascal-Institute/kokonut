# Kokonut Peer Discovery Architecture

## 🎯 Core Concept

**"Access the entire network with just one known node"**

- ❌ No hardcoded Fuel Nodes.
- ✅ You only need to know **one** node (either Fuel or Full).
- ✅ Download the blockchain from that peer.
- ✅ Scan the blockchain to discover all active Fuel Nodes.

---

## 🚀 Usage

### 1. Using Environment Variable (Recommended)

```bash
# Windows PowerShell
$env:KOKONUT_PEER="http://my-friend-node.com:80"
.\gradlew.bat :fullnode:run

# Linux/Mac
export KOKONUT_PEER=http://my-friend-node.com:80
./gradlew :fullnode:run
```

### 2. programmatic Configuration

```kotlin
// Initialize Full Node
fun main() {
    // Known peer address (Friend's node, Public node, etc.)
    BlockChain.initialize("http://known-node.example.com:80")
    
    // Start Server
    embeddedServer(Netty, host = "0.0.0.0", port = 80) {
        // ...
    }.start(true)
}
```

---

## 🔍 Bootstrap Process

```
1. Connect to Known Peer
   └─ GET http://known-node/getGenesisBlock
   
2. Download Genesis Block
   └─ Verify Network Rules
   
3. Download Blockchain
   └─ GET http://known-node/getChain
   
4. Scan Blockchain
   └─ Find Fuel Node Registration Blocks
   └─ Build Fuel Node List
   
5. Complete!
   └─ All active Fuel Nodes are now discovered
   └─ Ready to participate in the network
```

---

## 📊 Output Example

```
🔍 Bootstrapping from peer: http://friend-node.com:80
✅ Genesis Block downloaded: 000000abc123...
✅ Blockchain downloaded: 1523 blocks
✅ Found 5 Fuel Nodes in blockchain
   - http://fuel1.kokonut.io (stake: 1000000.0 KNT)
   - http://fuel2.kokonut.io (stake: 1500000.0 KNT)
   - http://fuel3.kokonut.io (stake: 2000000.0 KNT)
   - http://fuel4.kokonut.io (stake: 1200000.0 KNT)
   - http://fuel5.kokonut.io (stake: 1800000.0 KNT)
🎉 Bootstrap complete! Connected to Kokonut network.
```

---

## 🌐 Network Participation Scenarios

### Scenario 1: Joining via a Friend's Node

```kotlin
// Your friend is running a Full Node
val friendNode = "http://192.168.1.100:80"

// Bootstrap using their address
BlockChain.initialize(friendNode)

// You are now part of the global network!
```

### Scenario 2: Joining via a Public Node

```kotlin
// Publicly listed entry node
val publicNode = "http://public.kokonut.io:80"

BlockChain.initialize(publicNode)
```

### Scenario 3: Robust Initialization

```kotlin
val knownNodes = listOf(
    "http://node1.example.com:80",
    "http://node2.example.com:80",
    "http://node3.example.com:80"
)

for (node in knownNodes) {
    try {
        BlockChain.initialize(node)
        break  // Stop once successful
    } catch (e: Exception) {
        println("Failed to bootstrap from $node, trying next...")
    }
}
```

---

## ⚠️ Error Handling

### Case: No Fuel Nodes Found

```kotlin
try {
    val fuel = BlockChain.getRandomFuelNode()
} catch (e: IllegalStateException) {
    println(e.message)
    // Output:
    // No Fuel Nodes found. Please bootstrap from a known peer first.
    // Use: BlockChain.initialize("http://known-node-address")
    // Or set environment variable: KOKONUT_PEER=http://known-node-address
}
```

### Case: Bootstrap Failure

```kotlin
try {
    BlockChain.initialize("http://invalid-node.com")
} catch (e: Exception) {
    // Output:
    // ❌ Bootstrap failed: Connection refused
    //    Make sure the peer address is correct and accessible.
}
```

---

## 🎯 Key Advantages

### 1. **True Decentralization**
- Zero hardcoded addresses in the source code.
- Any node can serve as an entry point.

### 2. **Flexibility**
- Configurable via Environment Variables or Code.
- Supports multiple entry strategies.

### 3. **Scalability**
- New nodes are automatically discovered upon blockchain synchronization.
- No network size limits.

### 4. **Resilience**
- No single point of failure (SPOF) for bootstrapping.
- If one peer is down, simply try another.

---

## 🔧 Implementation Details

### BlockChain.kt

```kotlin
// Peer Discovery Logic
fun bootstrapFromPeer(peerAddress: String) {
    // 1. Download Genesis
    val genesis = URL(peerAddress).getGenesisBlock()
    
    // 2. Download Chain
    val chain = URL(peerAddress).getChain()
    
    // 3. Persist to DB
    chain.forEach { database.insert(it) }
    
    // 4. Scan for Fuel Nodes
    val fuels = scanFuelNodes()
}

// Dynamic Fuel Selection
fun getRandomFuelNode(): URL {
    val fuels = getFuelNodes()  // Returns cached list from scan
    if (fuels.isEmpty()) {
        throw IllegalStateException("Bootstrap first!")
    }
    return URL(fuels.random().address)
}
```

---

## 📝 Summary

| Feature | Legacy | Current Architecture |
|------|------|------|
| **Fuel Address** | Hardcoded | Blockchain Scan |
| **Entry Point** | Fixed Fuel Node | Any Peer |
| **Configuration** | Code Change Required | Environment Variable |
| **Scalability** | Limited | Unlimited |
| **Decentralization** | Partial | **Complete** |

**One Peer → Entire Network** 🌐
