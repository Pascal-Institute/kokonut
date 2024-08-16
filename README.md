[![](https://jitpack.io/v/Pascal-Institute/kokonut.svg)](https://jitpack.io/#Pascal-Institute/kokonut)
[![](https://jitpack.io/v/Pascal-Institute/kokonut/month.svg)](https://jitpack.io/#Pascal-Institute/kokonut)

# What is Kokonut...?

## Block Chain Framework powered by Kotlin

# Node

## Fuel Node

- URL : https://pascal-institute.github.io/kokonut-oil-station/
- Synchronize full nodes.
- Supplies important information to cross validation between full & light node

## Full Node

- URL :
  - https://kokonut.koyeb.app/ 
  - http://kokonut.iptime.org/
- Validate Block and Add to Chain. powered by kokonut
- Check Chain is valid

## Light Node

- Mine Block. powered by kokonut
- Check Chain is valid

# Kokonut Protocol 

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

- Proven Of Work is executed at least 1 full node
- Miner(64 digit hex string converted by public_key.pem) added to data block
- Reward added for miner(KNT) (Unfortunately, version 2 reward is Invalid)

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

- Proven Of Work is executed at least 1 full node
- Reward is valid (It is valuable)
- protocol version <= 2 blocks use proven of work of version 2 

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
