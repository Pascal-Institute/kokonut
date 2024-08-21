[![](https://jitpack.io/v/Pascal-Institute/kokonut.svg)](https://jitpack.io/#Pascal-Institute/kokonut)
[![](https://jitpack.io/v/Pascal-Institute/kokonut/month.svg)](https://jitpack.io/#Pascal-Institute/kokonut)

# What is Kokonut...?

## Block Chain Framework powered by Kotlin

# Node

## Fuel Node

- URL : https://pascal-institute.github.io/kokonut-oil-station/
- Synchronize full nodes
- Supplies important information to cross validation between full & light node

## Full Node

- URL :
  - node 0 : https://kokonut.koyeb.app/ 
  - node 1 : http://kokonut.iptime.org/
- Validate Block and Add to Chain. powered by kokonut
- Check Chain is valid

## Light Node

- Mine Block. powered by kokonut
- Check Chain is valid

# Kokonut Protocol 

## Definition

### Block

The block has unique 64-digit hex string hash

The version(it means protocol version) of next block is greater or equals to last block

The nonce means times of 1 cycle of proven of work

Block file format is JSON

Block file name is [hash].json for example, 000000000000000000000000000038a5bf10897e309c984402d1b8132faaaa.json

### Genesis Block

The block is 0 index of chain which has nonce : 0 & previous hash : "0"

The hash is made arificially

Any blocks can't be added before genesis block

## Version 1 (until kokonut 1.0.5)

### Abstract

Proven Of Work is executed at least 1 full node

### Block Structure

```json
{
  "version": 1,
  "index": 0,
  "previousHash": "00000000000000000000000000000000bf10897e359e874402dbb8132faaaa",
  "timestamp": 1722064910519,
  "ticker": "KNT",
  "data": {
    "comment": "When something is important enough, you do it even if the odds are not in your favor."
  },
  "difficulty": 32,
  "nonce": 12123345,
  "hash": "000000000000000000000000000038a5bf10897e309c984402d1b8132faaaa"
}
```

### Proven Of Work

```kotlin
val calculatedHash = Block.calculateHash(
  previousBlock.version,
  previousBlock.index,
  previousBlock.previousHash,
  currentBlock.timestamp,
  previousBlock.ticker,
  previousBlock.data,
  previousBlock.difficulty,
  currentBlock.nonce
  )

  fun calculateHash(version : Int, index: Long, previousHash: String, timestamp: Long, ticker: String, data: BlockData, difficulty: Int, nonce : Long): String {
    val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce"
      return MessageDigest.getInstance("SHA-256")
      .digest(input.toByteArray())
      .fold("") { str, it -> str + "%02x".format(it) }
  }
```

---

## Version 2 (until kokonut 1.0.7)

### Abstract

- Inherit abstract of version 2
- Miner(64 digit hex string converted by public_key.pem) added to block
- Reward added for miner(KNT) (Unfortunately, version 2 reward is Invalid)
- The minimum unit of reward is 0.000001

### Block Structure

```json
{
  "version":2,
  "index":13,
  "previousHash":"000000187dcf778ee1661b74744ab5b2fada3e4771a410f47624a2de63a4b60e",
  "timestamp":1723758087785,
  "ticker":"KNT",
  "data": {
    "miner":"6c60b7550766d5ae24ccc3327f0e47fbaa51e599172795bb9ad06ac82784a92d",
    "comment":"Mining Kokonut"
  },
  "difficulty":6,
  "nonce":11602138,
  "hash":"0000003564fa78d7c925342d2570700c9e2574bcbc5777db5d045b601d8dfe9a",
  "reward":50.0
}
```

### Proven Of Work (Same as Version 1)

---

## Version 3

### Abstract

- Inherit abstract of version 2
- Reward is valid (It is valuable)
- Protocol version smaller or equal to 2 blocks use version 2's proven of work 

### Block Structure (Same as Version 2)

### Proven Of Work

```kotlin
val calculatedHash = Block.calculateHash(
  previousBlock.version,
  previousBlock.index,
  previousBlock.previousHash,
  currentBlock.timestamp,
  previousBlock.ticker,
  previousBlock.data,
  previousBlock.difficulty,
  currentBlock.nonce,
  currentBlock.reward  
  )

  fun calculateHash(version : Int, index: Long, previousHash: String, timestamp: Long, ticker: String, data: BlockData, difficulty: Int, nonce : Long, reward : Double): String {
    val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce$reward"
      return MessageDigest.getInstance("SHA-256")
      .digest(input.toByteArray())
      .fold("") { str, it -> str + "%02x".format(it) }
  }
```

---

## Version 4

### Abstract

- Inherit abstract of version 3

#### Genesis Block

- miner is "0000000000000000000000000000000000000000000000000000000000000000"
- reward is 0.0

- KNT Transaction is available it is added to block
- Transaction Validation
  - Verify signed data with public key
  - Sender retention is bigger or equal than remittance
  - Sender and Receiver must be diffierent
- Tansaction State Diagram
  ![image](https://github.com/user-attachments/assets/9650ce7a-817b-4bc2-ba35-0bb458b8df27)
  1. Sender request transaction to fuel node with public key & signed data
  2. Transaction is added to transaction pool with state PENDING
  3. Fuel node execute validation using public key & signed data
  4. If transaction is valid, transaction state changes to READY
  5. Else if it is invalid, state is INVALID
  6. Miner picks transaction, state is RESERVED
  7. Mine is done, state is EXECUTED and it is recorded to Block
  8. Sender cancels transaction, state is CANCELED
  9. Miner drops out mining, state RESERVED to READY
  
### Block Structure

```json
{
  "version":4,
  "index":15,
  "previousHash":"000000187dcf778ee1661b74744ab5b2fada3e4771a410f47624a2de63a4b60e",
  "timestamp":1723758087785,
  "ticker":"KNT",
  "data": {
    "miner":"6c60b7550766d5ae24ccc3327f0e47fbaa51e599172795bb9ad06ac82784a92d",
    "comment":"Mining Kokonut"
    "transaction": [
      {
        "uid": "123e4567-e89b-12d3-a456-426614174000",
        "sender": "6c60b7550766d5ae24ccc3327f0e47fbaa51e599172795bb9ad06ac82784a92d",
        "receiver": "0000003564fa1117c925342d2570700c9e2574bcbc5777db5d045b601d8dfe9a",
        "remittance": 1.0,  
        "currency": "KNT"
      }
]
  },
  "difficulty":6,
  "nonce":11602138,
  "hash":"0000003564fa78d7c925342d2570700c9e2574bcbc5777db5d045b601d8dfe9a",
  "reward":50.0
}
```

### Proven Of Work

```kotlin
val calculatedHash = Block.calculateHash(
  currentBlock.version,
  previousBlock.index,
  previousBlock.previousHash,
  currentBlock.timestamp,
  previousBlock.ticker,
  previousBlock.data,
  previousBlock.difficulty,
  currentBlock.nonce,
  currentBlock.reward  
  )

  fun calculateHash(version : Int, index: Long, previousHash: String, timestamp: Long, ticker: String, data: BlockData, difficulty: Int, nonce : Long, reward : Double): String {
    val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce$reward"
      return MessageDigest.getInstance("SHA-256")
      .digest(input.toByteArray())
      .fold("") { str, it -> str + "%02x".format(it) }
  }
```
