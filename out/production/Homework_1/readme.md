### 前言

先是去Github上clone了一份助教的代码就开始改hhh。



### 关于`TxHandler.java`

要实现的三个方法分别做了下面三件事：

1. `public TxHandler(UTXOPool utxoPool)`

这是构造函数，我们的TxHandler需要一个UTXPool来验证交易的有效性，所以在初始化的时候传递进去。当然第三个方法有起到更新这个pool的作用。



2. `public boolean isValidTx(Transaction tx)`

验证交易的有效性，这个就不多说了，分别用控制流程块去判断那五种情况，有不对的地方就赶紧返回false，然后都通过了就返回true；



3. `public Transaction[] handleTxs(Transaction[] possibleTxs)`

这个地方因为交易是无序的，所以遍历一次肯定不行（有些交易在某另一些交易发生后才能生效），所以要用while一直循环直到找到跳出条件。这个地方我还看了一个别人写的版本，每次都把生效的交易先放进一个HashSet里，这样写看起来也比较优雅hhh。



**疑问**：我在写Test Case 的时候有一种情况，A作为创世区块先被放入了100个币，然后他进行了一次交易，转出去了一些币但没有转完。然而把这次交易通过`TxHandler.handleTxs`放入UTXOPool的时候，这第一个UTXO就被移除了。很奇怪，明明还有剩余的币，A应该可以继续用这个UTXO作为凭证继续交易的。

那么这个没花完的UTXO要么在`.handleTxs`不应该移除，要么在`.isValidTx`里的校验方式应该去遍历后来的花费？

因为我上课的时候UTXO这一块~~没有好好听~~没搞明白，所以不确定这样理解有没有问题，也就没有去改代码。



### 关于`TxHandlerTest.java`

一共写了8个测试用例，每个@Test方法的上面都写了注释，类似下面这样，

```java
/*
* Test 6: First A transfer 100 to B, but the transaction has 
* not been put into UTXO. At that time B wants
* to transfer some coins to C. This should be a false transaction.
* */
```

这个地方有一个操作我写在类开头的注释了，主要是写了下面两个函数，避免代码块大量重复，

- `helperInit()`

这个函数用来初始化创世区块，在我们的测试用例中总是先给A用户100个币，然后把这次创世交易放在UTXOPool里。

- `helperSign（）`

这个函数用来替代签名的那一串方法，`getInstance()`，`initSign()`什么的，全部封装在这里面的，这样到时候签名的时候就只用一行来调用这个函数就行了。这样后面的Test可读性有提高，因为代码几乎都是声明式的，每一行做一件事，比如`addinput`，`addOutput`或者`addSignature`什么的。



### 吐槽

区块链好难，OOP的写法很麻烦，作用域不清楚，然后强弱类型搞的各种转换。最应该吐槽的是，Java的历史包袱太重了，从6到8到11到现在不知道多少了，没有好的polyfill，导致不得不用各种try...catch语句去兼容版本。（以上都是猜测，或许有好的解决办法只是我不了解这个语言吧）。over。
