package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.scalar.db.api.TwoPhaseCommitTransaction;
import com.scalar.db.api.TwoPhaseCommitTransactionManager;
import com.scalar.db.exception.transaction.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for Two-Phase Commit (2PC) services
 *
 * Provides common lifecycle methods for 2PC protocol:
 * - prepare: Prepares the transaction for commit
 * - validate: Validates the transaction state
 * - commit: Commits the transaction
 * - rollback: Rolls back the transaction
 *
 * All lifecycle methods use manager.resume(transactionId) to access the transaction.
 */
@Slf4j
public abstract class BaseTwoPCService {
    protected TwoPhaseCommitTransactionManager manager;

    public BaseTwoPCService(TwoPhaseCommitTransactionManager manager) {
        this.manager = manager;
    }

    /**
     * Prepare phase - locks resources and prepares for commit
     */
    public ResponseStatusDto prepare(String transactionId) throws CustomException {
        try {
            TwoPhaseCommitTransaction transaction = manager.resume(transactionId);
            transaction.prepare();
            log.info("Transaction prepared: {}", transactionId);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("Prepare failed: {}", e.getMessage(), e);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    /**
     * Validate phase - verifies transaction can be committed
     */
    public ResponseStatusDto validate(String transactionId) throws CustomException {
        try {
            TwoPhaseCommitTransaction transaction = manager.resume(transactionId);
            transaction.validate();
            log.info("Transaction validated: {}", transactionId);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("Validate failed: {}", e.getMessage(), e);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    /**
     * Commit phase - commits the transaction
     */
    public ResponseStatusDto commit(String transactionId) throws CustomException {
        try {
            TwoPhaseCommitTransaction transaction = manager.resume(transactionId);
            transaction.commit();
            log.info("Transaction committed: {}", transactionId);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("Commit failed: {}", e.getMessage(), e);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    /**
     * Rollback phase - rolls back the transaction
     */
    public ResponseStatusDto rollback(String transactionId) throws CustomException {
        try {
            TwoPhaseCommitTransaction transaction = manager.resume(transactionId);
            transaction.rollback();
            log.info("Transaction rolled back: {}", transactionId);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found for rollback: {}", transactionId);
            return ResponseStatusDto.builder().code(0).message("Transaction not found").build();
        } catch (Exception e) {
            log.error("Rollback failed: {}", e.getMessage(), e);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    /**
     * Handle transaction exceptions and attempt rollback
     */
    protected void handleTransactionException(Exception e, TwoPhaseCommitTransaction transaction) {
        log.error(e.getMessage(), e);
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (RollbackException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Determine error code based on exception type
     */
    protected int determineErrorCode(Exception e) {
        if (e instanceof UnsatisfiedConditionException) return 9100;
        if (e instanceof UnknownTransactionStatusException) return 9200;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
