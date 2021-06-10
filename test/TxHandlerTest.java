package test;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import org.junit.BeforeClass;

import src.*;

import org.junit.Test;


public class TxHandlerTest {
    static KeyPair kpA, kpB, kpC, kpD;
    static KeyPairGenerator kpg;

    /*
     * Note that before any test we need to initialize to create some virtual transactors
     * like A, B, C and D.
     * */

    @BeforeClass
    public static void beforeClass() {
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpA = kpg.generateKeyPair();
            kpB = kpg.generateKeyPair();
            kpC = kpg.generateKeyPair();
            kpD = kpg.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    /*
     * We have two helper functions in this test class.
     *  1. helperInit(): This function helps us to create the genesis transaction that gives 100 coins
     *                   to A and put this transaction into UTXOpool so that we can validate later ones.
     *
     * 2. helperSign(): This functions helps to make signature on the transaction, since the procedure is
     *                  so common in every test that we want to simplify our codes by put them in one function.
     *
     * */

    /*
     * This class is useful to initialize and return the genesis transaction and the pool.
     * We will start each test by get the initialized variables.*/
    class Init {
        Transaction genesisTx;
        UTXOPool pool;
    }

    public Init helperInit() {
        // genesis block
        Transaction genesisTx = new Transaction();
        genesisTx.addOutput(100, kpA.getPublic());
        genesisTx.finalize();
        // set UTXO of genesis block
        UTXOPool pool = new UTXOPool();
        UTXO utxo = new UTXO(genesisTx.getHash(), 0);
        pool.addUTXO(utxo, genesisTx.getOutput(0));
        // return value
        Init init = new Init();
        init.genesisTx = genesisTx;
        init.pool = pool;
        return init;
    }

    public byte[] helperSign(Transaction tx, java.security.PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // sign the transaction
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(tx.getRawDataToSign(0));
        return sig.sign();
    }

    /*
     * Test 1 is to make sure we can generate the UTXOPool and the TxHandler correctly.
     * */
    @Test
    public void testInit() {
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;

        TxHandler handler = new TxHandler(pool);

        assertNotNull(genesisTx);
        assertNotNull(pool);
        assertNotNull(handler);
    }

    /*
     * Test 2 is to make sure we can finish a simple transaction.
     * */
    @Test
    public void testSimpleTx() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction tx = new Transaction();
        tx.addInput(genesisTx.getHash(), 0);
        tx.addOutput(50, kpB.getPublic());
        tx.addOutput(20, kpB.getPublic());
        tx.addOutput(10, kpB.getPublic());
        // sign the transaction
        tx.getInput(0).addSignature(helperSign(tx, kpA.getPrivate()));
        tx.finalize();
        // test the result
        assertTrue(handler.isValidTx(tx));
    }

    /*
     * Test 3: let's test that if A wants to transfer coins to itself
     * with less or more coins that A has.
     * */
    @Test
    public void testTransferToSelf() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction: from A to self
        TxHandler handler = new TxHandler(pool);
        Transaction tx20 = new Transaction();
        tx20.addInput(genesisTx.getHash(), 0);
        tx20.addOutput(20, kpA.getPublic());
        // sign the transaction
        tx20.getInput(0).addSignature(helperSign(tx20, kpA.getPrivate()));
        tx20.finalize();
        // this can be true because 20 < 100
        assertTrue(handler.isValidTx(tx20));


        // make another transaction: transfer more money that A has
        Transaction tx1000 = new Transaction();
        tx1000.addInput(genesisTx.getHash(), 0);
        tx1000.addOutput(1000, kpA.getPublic());
        // sign the transaction
        tx1000.getInput(0).addSignature(helperSign(tx1000, kpA.getPrivate()));
        tx1000.finalize();
        // this can not be true because A only has 100 coins
        assertFalse(handler.isValidTx(tx1000));
    }

    /*
     * Test 4: test if B can steal money from A with B's own signature.
     * */
    @Test
    public void testStealMoney() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // launch a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction tx = new Transaction();
        tx.addInput(genesisTx.getHash(), 0);
        tx.addOutput(50, kpB.getPublic());
        // sign the transaction, but with kpB
        tx.getInput(0).addSignature(helperSign(tx, kpB.getPrivate()));
        tx.finalize();
        // test the result
        assertFalse(handler.isValidTx(tx));
    }

    /*
     * Test 5: "A" first transfer coins to B, then transfer the same coins to C.
     * We mush make sure that the second spending is invalid;
     * */
    @Test
    public void testMoneyNotEnough() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction txAtoB = new Transaction();
        txAtoB.addInput(genesisTx.getHash(), 0);
        txAtoB.addOutput(100, kpB.getPublic());
        // sign the transaction
        txAtoB.getInput(0).addSignature(helperSign(txAtoB, kpA.getPrivate()));
        txAtoB.finalize();
        // test the transaction and add it to UTXO transaction handlers
        assertTrue(handler.isValidTx(txAtoB));
        handler.handleTxs(new Transaction[]{txAtoB});

        // try to transfer the same money to C
        Transaction txAtoC = new Transaction();
        txAtoC.addInput(genesisTx.getHash(), 0);
        txAtoC.addOutput(100, kpC.getPublic()); // actually, only 50 coins left
        // sign the transaction
        txAtoC.getInput(0).addSignature(helperSign(txAtoC, kpA.getPrivate()));
        txAtoC.finalize();
        // now it should be invalid because the money has go to B
        assertFalse(handler.isValidTx(txAtoC));
    }

    /*
    * Test 6: First A transfer 100 to B, but the transaction has not been put into UTXO. At that time B wants
    * to transfer some coins to C. This should be a false transaction.
    * */
    @Test
    public void testNotInUTXO() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction txAtoB = new Transaction();
        txAtoB.addInput(genesisTx.getHash(), 0);
        txAtoB.addOutput(100, kpB.getPublic());
        // sign the transaction
        txAtoB.getInput(0).addSignature(helperSign(txAtoB, kpA.getPrivate()));
        txAtoB.finalize();
        assertTrue(handler.isValidTx(txAtoB));

        // now B wants to transfer to C but the transaction before has not be put into UTXOpool
        Transaction txAtoC = new Transaction();
        txAtoC.addInput(txAtoB.getHash(), 0);
        txAtoC.addOutput(10, kpC.getPublic());
        // sign the transaction
        txAtoC.getInput(0).addSignature(helperSign(txAtoC, kpB.getPrivate()));
        txAtoC.finalize();

        assertFalse(handler.isValidTx(txAtoC));
    }

    /*
    * Test 7: This a little more complex example. We have a scene that A transfer 30, 20 and 10
    * respectively to B, C and D. We put the transaction into UTXO. Then we have 3 transactions.
    * The first is B to C 20, this should be valid because B has 30 coins;
    * The second is C to D 20, this also should be valid because C has 20 coins;
    * The third is D to A 20, this should not be valid because D only has 10 coins.
    * */

    @Test
    public void testCountValidTransaction() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction tx = new Transaction();
        tx.addInput(genesisTx.getHash(), 0);
        tx.addOutput(30, kpB.getPublic());
        tx.addOutput(20, kpC.getPublic());
        tx.addOutput(10, kpD.getPublic());
        // sign the transaction
        tx.getInput(0).addSignature(helperSign(tx, kpA.getPrivate()));
        tx.finalize();

        assertEquals(handler.handleTxs(new Transaction[]{tx}).length, 1);

        // valid 1: B to C 20 coins
        Transaction txBtoC = new Transaction();
        txBtoC.addInput(tx.getHash(), 0);
        txBtoC.addOutput(20, kpC.getPublic());
        txBtoC.getInput(0).addSignature(helperSign(txBtoC, kpB.getPrivate()));
        txBtoC.finalize();
        // valid 2: C to D 20 coins
        Transaction txCtoD = new Transaction();
        txCtoD.addInput(tx.getHash(), 1);
        txCtoD.addOutput(20, kpD.getPublic());
        txCtoD.getInput(0).addSignature(helperSign(txCtoD, kpC.getPrivate()));
        txCtoD.finalize();
        // invalid 3: D to A 20 coins
        Transaction txDtoA = new Transaction();
        txDtoA.addInput(tx.getHash(), 2);
        txDtoA.addOutput(20, kpA.getPublic());
        txDtoA.getInput(0).addSignature(helperSign(txDtoA, kpD.getPrivate()));
        txDtoA.finalize();

        // so only 2 transactions are valid
        assertEquals(handler.handleTxs(new Transaction[]{txBtoC, txCtoD, txDtoA}).length, 2);
    }

    /*
    * Test 8: double-spending. Let's write the code directly.*/
    @Test
    public void testDoubleSpending() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction tx = new Transaction();
        tx.addInput(genesisTx.getHash(), 0);
        tx.addOutput(100, kpB.getPublic());
        tx.addOutput(100, kpC.getPublic());
        // sign the transaction
        tx.getInput(0).addSignature(helperSign(tx, kpA.getPrivate()));
        tx.finalize();

        assertFalse(handler.isValidTx(tx));
    }

    /*
    *  Test 9: transfer negative. To test if A can transfer a negative value coin to B.
    *  This must now be allowed.
    * */
    @Test
    public void testTransferNegative() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // init block
        Init init = helperInit();
        Transaction genesisTx = init.genesisTx;
        UTXOPool pool = init.pool;
        // make a transaction from A to B
        TxHandler handler = new TxHandler(pool);
        Transaction tx = new Transaction();
        tx.addInput(genesisTx.getHash(), 0);
        tx.addOutput(-100, kpB.getPublic());
        // sign the transaction
        tx.getInput(0).addSignature(helperSign(tx, kpA.getPrivate()));
        tx.finalize();

        // since the value is negative, this mush be false;
        assertFalse(handler.isValidTx(tx));
    }
}