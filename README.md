# What is Kokonut...?

## Block Chain Framework powered by Kotlin

# Node

## Fuel Node

It supplies important informations to cross validation between full & light node

https://pascal-institute.github.io/kokonut-oil-station/

## Full Node

Validate Block and Add to Chain. powered by kokonut

http://kokonut.iptime.org/

## Light Node

Mining Block. powered by kokonut

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

```
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
  "version": 2,
  "index": 1,
  "previousHash": "00000000000000000000000000000000bf10897e359e874402dbb8132faaaa",
  "timestamp": 1722064910519,
  "ticker": "KNT",
  "data": {
    "miner": "000000b64827459403e88439e2b4764874199f8e1810a1cd630c7e8432342368",
    "comment": "When something is important enough, you do it even if the odds are not in your favor."
  },
  "difficulty": 8,
  "nonce": 12123345,
  "hash": "000000000000000000000000000038a5bf10897e309c984402d1b8132faaaa",
  "reward": 50.0
}
```

### Proven Of Work (Same to Version 1)