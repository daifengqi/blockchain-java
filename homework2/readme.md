### Solution Summary

This homework requires to implement the method inside the class `BlockChain`, which has a internal class `BlockNode`, representing each block inside the chain. We use a hash map to contain the nodes, that means, the blockchain is a set of block nodes. We have 4 steps inside the constructor of blockchain,

1. add coinbase utxo into utxopool
2. create a blockNode with the given block (genesisBlock)
3. register the node into the blockChain hashmap.
4. maintian or update the maxHeightNode and oldestBlockHeight

Then we have some methods to return the internal attributes, like `getMaxHeightBlock` and `getOldestBlockHeight`.

The `addBlock` method is important so that it is responsible for add new block to the chain. It should finish all the tasks below,

- the the parent node's hash
- check all the transactions in the new block
- check the length of current branch and cut it if it's too short
- put the coinbase into UTXOPool as reward to miner
- connect the new block to the chain
- finally we want to make sure that we only keep the recent block

And we also have a `BlockHandler` class responsible for adding new blocks utilizing the `addBlock` method.



### What each test do

```java
// 1
void testEmptyBlock()
```

Test 1 is to generate a genesis Block and put it into the chain. The test should return True if it is successfully put.

```java
//2
void testValidTx()
```

Test 2 is to add an valid transaction into a block, then put this block into blockchain. It is expected to return True because this transaction is valid.

```java
// 3
void testInvalidTx()
```

Test 3. This test has two invalid transactions. Firstly, it has a transaction with 10+25 output when the coinbase reward is only 25. This transaction is invalid so that the first test should return false. The second is that when the first coinbase reward has been spent, it wants to use it again, so the next transaction should be invalid which should asert to be false.

```java
// 4
void testPrevBlockHash()
```

Test 4 is to test that a new block can be added to the block chain when the previous hash is right or wrong. It should assert false when the previous hash is incorrect but ture when it is correct.

```java
// 5
void testCreateMultiBlocks()
```

Test 5 is to test when including a transaction into a block, this transaction and the block could only be added to the chain once. When another miner (like C) want to add the transaction, it should return nothing.

```java
// 6
void testCreateAfterProcess()
```

Test 6 is to make sure that we call still create new block after we have put a block into the chain, and the newly created block's previous hash is equal to the block we have put into the chain before.

```java
// 7
void testMemoryMaintain()
```

Test 7 is to test the `getOldestBlockHeight()` can return the true blockchain height.
