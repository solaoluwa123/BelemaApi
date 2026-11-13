package com.transgate.api.app.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.ResponseManager;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

class ResponseTransactionsTest {

    private final ResponseManager responses = new ResponseManager();

    private static NetworkResponse network(String status) {
        NetworkResponse body = new NetworkResponse();
        body.setStatus(status);
        return body;
    }

    private static LoginResponse login(String status) {
        LoginResponse body = new LoginResponse();
        body.setStatus(status);
        return body;
    }

    @Test
    void successResponsesCommit() {
        assertTrue(ResponseTransactions.isSuccess(responses.ResponseAccepted()));
        assertTrue(ResponseTransactions.isSuccess(responses.ResponseOk(login("success"))));
    }

    @Test
    void failureResponsesRollBack() {
        assertFalse(ResponseTransactions.isSuccess(responses.ResponseOk(network("failed"))));
        assertFalse(ResponseTransactions.isSuccess(responses.ResponseOk(login("failed"))));
        assertFalse(ResponseTransactions.isSuccess(responses.ResponseInternalServerError()));
        assertFalse(ResponseTransactions.isSuccess(responses.ResponseBadRequest()));
        assertFalse(ResponseTransactions.isSuccess(responses.ResponseUnathorized()));
        assertFalse(ResponseTransactions.isSuccess(responses.ResponseNotFound(network("failed"))));
        assertFalse(ResponseTransactions.isSuccess(null));
    }

    @Test
    void marksTheTransactionRollbackOnlyWhenTheResponseFails() {
        AtomicReference<TransactionStatus> status = new AtomicReference<>();
        TransactionOperations recording = new TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                status.set(new SimpleTransactionStatus());
                return action.doInTransaction(status.get());
            }
        };
        ResponseTransactions transactions = new ResponseTransactions(recording);

        ResponseEntity failed = responses.ResponseOk(network("failed"));
        assertSame(failed, transactions.run(() -> failed));
        assertTrue(status.get().isRollbackOnly());

        ResponseEntity ok = new ResponseEntity(network("success"), HttpStatus.OK);
        assertSame(ok, transactions.run(() -> ok));
        assertFalse(status.get().isRollbackOnly());
    }
}
