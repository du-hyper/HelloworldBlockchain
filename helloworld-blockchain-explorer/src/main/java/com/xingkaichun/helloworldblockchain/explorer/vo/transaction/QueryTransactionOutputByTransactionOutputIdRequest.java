package com.xingkaichun.helloworldblockchain.explorer.vo.transaction;

import com.xingkaichun.helloworldblockchain.core.model.transaction.TransactionOutputId;

public class QueryTransactionOutputByTransactionOutputIdRequest {

    TransactionOutputId transactionOutputId;


    public TransactionOutputId getTransactionOutputId() {
        return transactionOutputId;
    }

    public void setTransactionOutputId(TransactionOutputId transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
