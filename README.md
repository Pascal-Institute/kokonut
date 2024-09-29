[![](https://jitpack.io/v/Pascal-Institute/kokonut.svg)](https://jitpack.io/#Pascal-Institute/kokonut)
[![](https://jitpack.io/v/Pascal-Institute/kokonut/month.svg)](https://jitpack.io/#Pascal-Institute/kokonut)
![Docker Pulls](https://img.shields.io/docker/v/volta2030/knt_fullnode?label=knt_fullnode&sort=semver)

# What is Kokonut...?

## BlockChain Framework powered by Kotlin

# Kokonut Protocol

## Version : 4

## Abstract

This protocol describes block chain systems & rules of kokonut.

## Block

- The 'Block' is the collection of data which contains uniqueness.
- Blocks have meaning and value only when they are connected to each other.
- Format is JSON(.json).

### Basic Structure

- version : The kokonut protocol verison (match to major number of library version).
- index : The numbering of block which previous block index < next block index.
- previousHash : The hash of previous block.
- timestamp : Time relative to UTC. Indicates the time the block difficulty was solved.
- data : 
- difficulty : The numbers of leading 0 of hash. All block hash must follow difficulty.
- nonce : The total times of trying hash function for satisfying difficulty.
- hash : The output 64-digits string format which came from SHA-256(Secure Hash Algorithm 256-bit) hash function, inputs is the sequential collection of all data's in block.

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

## BlockChain

- The connectivity of blocks.

### Kovault

- Kovalut is Database which use SQLite DBMS(DataBase Management System) software.

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
- Configuration is 3 parts. Genesis, Full and Light Nodes

### Genesis Node

### Fuel Node (Will be deprecated)

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

## Propagation

- It has two mission. 1. Alert stop other full nodes mining 2. Comparison other full nodes than
  infect longer blockchain.

### Diagram
![image](https://github.com/user-attachments/assets/e9fe5f3e-e0d6-4410-a43a-13ca6d792fb8)

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
