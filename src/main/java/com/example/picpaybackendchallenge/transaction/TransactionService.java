package com.example.picpaybackendchallenge.transaction;

import com.example.picpaybackendchallenge.authorization.AuthorizerService;
import com.example.picpaybackendchallenge.notification.NotificationService;
import com.example.picpaybackendchallenge.wallet.WalletRepository;
import com.example.picpaybackendchallenge.wallet.WalletType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuthorizerService authorizerService;
    private final NotificationService notificationService;

    public TransactionService(TransactionRepository transactionRepository, WalletRepository walletRepository, AuthorizerService authorizerService, NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.authorizerService = authorizerService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        // 1 - Validate transaction
        validate(transaction);

        // 2 - Create transaction
        var newTransaction = transactionRepository.save(transaction);

        // 3 - Debit from payer wallet
        var walletPayer = walletRepository.findById(transaction.payer()).get();
        walletRepository.save(walletPayer.debit(transaction.value()));

        // 4- Credit on payee wallet
        var walletPayee = walletRepository.findById(transaction.payee()).get();
        walletRepository.save(walletPayer.credit(transaction.value()));

        // 5 - Call external services
        // Authorize transaction
        authorizerService.authorize(transaction);

        // Notification
        notificationService.notify(transaction);

        return newTransaction;
    }

    /*
     * A transaction is valid if:
     * - the payer is a common wallet
     * - the payer has enough balance
     * - the payer is not the payee
     */

    private void validate(Transaction transaction) {
        walletRepository.findById(transaction.payee())
                .map(payee -> walletRepository.findById(transaction.payer())
                        .map(
                                payer -> payer.type() == WalletType.COMUM.getValue() &&
                                        payer.balance().compareTo(transaction.value()) >= 0 &&
                                        !payer.id().equals(transaction.payee()) ? true : null)
                        .orElseThrow(() -> new InvalidTransactionException(
                                "Invalid transaction - " + transaction)))
                .orElseThrow(() -> new InvalidTransactionException(
                        "Invalid transaction - " + transaction));
    }

    public List<Transaction> list() {
        return transactionRepository.findAll();
    }

}
