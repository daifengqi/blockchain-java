package block_chain;

import java.security.PublicKey;

public class BlockHandler {
    private BlockChain blockChain;

    /**
     * assume blockChain has the genesis block
     */
    public BlockHandler(BlockChain blockChain) {
        this.blockChain = blockChain;
    }

    /**
     * add {@code block} to the block chain if it is valid.
     *
     * @return true if the block is valid and has been added, false otherwise
     */
    public boolean processBlock(Block block) {
        if (block == null)
            return false;
        return blockChain.addBlock(block);
    }

    /**
     * create a new {@code block} over the max height {@code block}
     */
    public Block createBlock(PublicKey myAddress) {
        Block parent = blockChain.getMaxHeightBlock();


        byte[] parentHash = parent.getHash();

        Block current = new Block(parentHash, myAddress);

        TransactionPool txPool = blockChain.getTransactionPool();

        Transaction[] txs = txPool.getTransactions().toArray(new Transaction[0]);


        for (int i = 0; i < txs.length; i++)
            current.addTransaction(txs[i]);

        current.finalize();
        if (blockChain.addBlock(current))
            return current;
        else
            return null;
    }

    /**
     * process a {@code Transaction}
     */
    public void processTx(Transaction tx) {
        blockChain.addTransaction(tx);
    }
}
