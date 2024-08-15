## full node

- http://kokonut.iptime.org/

## proven of work
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

## protocol version 1 (until kokonut 1.0.5)
```json
{
  "version": 1,
  "index": 1,
  "previousHash": "00000000000000000000000000000000bf10897e359e874402dbb8132faaaa",
  "timestamp": 1722064910519,
  "ticker": "KNT",
  "data": {
    "comment": "When something is important enough, you do it even if the odds are not in your favor."
  },
  "difficulty": 8,
  "nonce": 12123345,
  "hash": "000000000000000000000000000038a5bf10897e309c984402d1b8132faaaa"
}
```

## protocol version 2
- miner added to data block
- reward added for miner(KNT)

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
