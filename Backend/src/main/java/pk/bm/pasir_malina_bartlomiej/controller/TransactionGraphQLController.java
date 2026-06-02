package pk.bm.pasir_malina_bartlomiej.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import pk.bm.pasir_malina_bartlomiej.dto.BalanceDTO;
import pk.bm.pasir_malina_bartlomiej.dto.TransactionDTO;
import pk.bm.pasir_malina_bartlomiej.model.Transaction;
import pk.bm.pasir_malina_bartlomiej.model.User;
import pk.bm.pasir_malina_bartlomiej.service.TransactionService;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.bm.pasir_malina_bartlomiej.service.UserService;

import java.util.List;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionGraphQLController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public BalanceDTO userBalance(@Argument Double days) {
        User user = userService.getAuthenticatedUser();
        return transactionService.getUserBalance(user, days);
    }

    @MutationMapping
    public Transaction addTransaction(
            @Valid @Argument(name = "transactionDTO") TransactionDTO transactionDTO) {
        return transactionService.createTransaction(transactionDTO);
    }

    @MutationMapping
    public Transaction updateTransaction(
            @Argument Long id,
            @Valid @Argument(name = "transactionDTO") TransactionDTO transactionDTO) {
        return transactionService.updateTransaction(id, transactionDTO);
    }

    @MutationMapping
    public Boolean deleteTransaction(@Argument Long id) {
        try {
            transactionService.deleteTransaction(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}