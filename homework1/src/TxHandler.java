package src;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
     * by using the UTXOPool(UTXOPool uPool) constructor.
     */
    private final UTXOPool curUTXOPool;

    // constructor
    public TxHandler(UTXOPool utxoPool) {
        this.curUTXOPool = utxoPool;
    }

    /**
     * @return true if: (1) all outputs claimed by {@code tx} are in the current
     * UTXO pool, (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx}, (4) all of
     * {@code tx}s output values are non-negative, and (5) the sum of
     * {@code tx}s input values is greater than or equal to the sum of its
     * output values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        // initialize parameters
        double totalOutValue = 0;
        double totalInValue = 0;
        HashSet<UTXO> uSet = new HashSet<>();

        // iterate through transaction's input
        for (int index = 0; index < tx.getInputs().size(); ++index) {
            Transaction.Input txIn = tx.getInput(index);
            byte[] inPrevHash = txIn.prevTxHash;
            int inOutputIndex = txIn.outputIndex;

            UTXO utxo = new UTXO(inPrevHash, inOutputIndex);

            // (1).the UTXO must in utxoPool
            if (!this.curUTXOPool.contains(utxo)) {
                return false;
            }

            // (2). check the signature
            PublicKey pk = this.curUTXOPool.getTxOutput(utxo).address;
            if (!Crypto.verifySignature(pk, tx.getRawDataToSign(index), txIn.signature)) {
                return false;
            }

            // (3). no repeat (use hashMap to make sure this)
            if (!uSet.contains(utxo)) {
                uSet.add(utxo);
                totalInValue += this.curUTXOPool.getTxOutput(utxo).value;
            } else {
                return false;
            }
        }

        // iterate through transaction's output
        for (Transaction.Output txout : tx.getOutputs()) {
            // (4). output value must be non-negative
            if (txout.value >= 0) {
                totalOutValue += txout.value;
            } else {
                return false;
            }
        }

        // 5. finally, total input value mush >= output value
        return totalInValue >= totalOutValue;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions,
     * checking each transaction for correctness, returning a mutually valid array
     * of accepted transactions, and updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // transfer input
        ArrayList<Transaction> possibleTxsList = new ArrayList<>(Arrays.asList(possibleTxs));
        // initialize an ArrayList to save valid transactions
        ArrayList<Transaction> validTx = new ArrayList<>();

        /* important! The transactions are not ordered, so we may need to iterate the transaction list
         * many times to make sure all valid transactions are handled */
        while (true) {
            int initSize = possibleTxsList.size();
            List<Transaction> toRemove = new ArrayList<>();
            // iterate through each transaction once
            for (Transaction aTran : possibleTxsList) {
                // if a transaction is valid, we need to change the owner of coins
                if (isValidTx(aTran)) {
                    // delete the inputs (the original owner does not hold the coins anymore)
                    for (Transaction.Input txIn : aTran.getInputs()) {
                        byte[] inPrevHash = txIn.prevTxHash;
                        int inOututIndex = txIn.outputIndex;
                        UTXO toDelUtxo = new UTXO(inPrevHash, inOututIndex);
                        this.curUTXOPool.removeUTXO(toDelUtxo);
                    }

                    // add the outputs (to the new owners)
                    /* note that there can be more than one outputs in one transaction,
                     * so we need an index to distinguish them.*/
                    int index = 0;
                    byte[] aTranHash = aTran.getHash();
                    for (Transaction.Output txOut : aTran.getOutputs()) {
                        UTXO utxo = new UTXO(aTranHash, index);
                        this.curUTXOPool.addUTXO(utxo, txOut);
                        index++;
                    }

                    validTx.add(aTran);
                    toRemove.add(aTran);
                }
            }
            possibleTxsList.removeAll(toRemove);
            // condition to break the loop: either the size is unchanged or all transactions are handled
            if (possibleTxsList.size() == initSize || possibleTxsList.size() == 0) {
                break;
            }
        }

        return validTx.toArray(new Transaction[0]);
    }

}
