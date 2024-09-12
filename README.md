[![](https://jitpack.io/v/Pascal-Institute/kokonut.svg)](https://jitpack.io/#Pascal-Institute/kokonut)
[![](https://jitpack.io/v/Pascal-Institute/kokonut/month.svg)](https://jitpack.io/#Pascal-Institute/kokonut)

# What is Kokonut...?

## Block Chain Framework powered by Kotlin

# Kokonut Protocol

## Version : 4

## Abstract

This protocol describes block chain systems & rules of kokonut

## Block

### Basic Structure

- version : The kokonut protocol verison (match to major number of library version)
- index : The numbering of block which previous block index < next block index
- previousHash : The hash of previous block
- timestamp
- data
- difficulty
- nonce
- hash

```json
{
  "version":4,
  "index":1,
  "previousHash":"00000000000000000000000000000000000000000000000061bdff5e59b8ff4c",
  "timestamp":1724547179867,
  "data": {
    "reward":16.230218,
    "miner":"6c60b7550766d5ae24ccc3327f0e47fbaa51e599172795bb9ad06ac82784a92d",
    "transactions":[],
    "comment":"kokonut version 4"
    },
  "difficulty":6,
  "nonce":1502929,
  "hash":"000000f31571551dacab93769546843d2ef483fd0d26181fe8950de617b919ec"}
```

### Genesis Block

It is called genesis block which follows below :

- first block of chain
- previousHash is "0"
- reward is 0
- miner is "0000000000000000000000000000000000000000000000000000000000000000"
- transactions is empty
- difficulty is 0
- nonce is 0
- hash isn't made by SHA-256 Hash Algorithm. it is artificially generated.

#### Structure

```
{ 
  "version":4,
  "index":0,
  "previousHash":"0",
  "timestamp":1725108420520,
  "data":{
    "reward":0.0,
    "ticker":"KNT",
    "miner":"0000000000000000000000000000000000000000000000000000000000000000",
    "transactions":[],
    "comment":"Navigate beyond computing oceans"
   },
  "difficulty":0,
  "nonce":0,
  "hash":"000000000000000000000000000000000000000000000000190282d71244ac7a"
}
```
## Proven Of Work

### Validation Process
 1. Check Miner
 2. Check Index
 3. Check Version
 4. CHeck Difficulty
 5. Check Hash

### Calculate Hash
```kotlin
    fun calculateHash(): String {
        val input = "$version$index$previousHash$timestamp$data$difficulty$nonce"
        hash = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
        return hash
    }

```

## Mining

### Status

* READY
* MINING
* FAILED
* MINED

### State Machine Diagram

![image](https://github.com/user-attachments/assets/d53c3d55-3678-4489-a250-5a7bea3d92ee)


### Difficulty

### Reward

#### Reduction Time
![image](https://github.com/user-attachments/assets/631d3d02-c8c6-491f-8ed0-073b11eb8fd5)

## Node
- Configuration is 4 parts. Fuel, Full and Light Nodes

### Genesis Node

### Fuel Node

- URL : https://kokonut-oil.onrender.com
- Synchronize full nodes
- Supplies important information to cross validation between full & light node

### Full Node

- URL :
- DockerHub : https://hub.docker.com/r/volta2030/knt_fullnode
- Validate Block and Add to Chain. powered by kokonut
- Check Chain is valid

### Light Node

- Mine Block. powered by kokonut
- Check Chain is valid

## Transaction

### Status

* INVALID
* PENDING
* READY
* RESERVED
* EXECUTED

### State Machine Diagram

![image](https://github.com/user-attachments/assets/2f09706d-d207-416b-bd93-6955b2ff7850)

## Wallet
