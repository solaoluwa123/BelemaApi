package com.transgate.api.app.services;

import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.NetworkResponse;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a service call that reports failure through its {@link ResponseEntity} (rather than by throwing)
 * in one database transaction, and rolls the transaction back unless the response is a success.
 */
@Component
public class ResponseTransactions {

    private final TransactionOperations transactions;

    @Autowired
    public ResponseTransactions(PlatformTransactionManager transactionManager) {
        this(new TransactionTemplate(transactionManager));
    }

    ResponseTransactions(TransactionOperations transactions) {
        this.transactions = transactions;
    }

    public ResponseEntity run(Supplier<ResponseEntity> work) {
        return transactions.execute(status -> {
            ResponseEntity response = work.get();
            if (!isSuccess(response)) {
                status.setRollbackOnly();
            }
            return response;
        });
    }

    static boolean isSuccess(ResponseEntity response) {
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            return false;
        }
        Object body = response.getBody();
        if (body instanceof NetworkResponse) {
            return "success".equalsIgnoreCase(((NetworkResponse) body).getStatus());
        }
        if (body instanceof LoginResponse) {
            return "success".equalsIgnoreCase(((LoginResponse) body).getStatus());
        }
        return true;
    }
}
