package pk.bm.pasir_malina_bartlomiej.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.bm.pasir_malina_bartlomiej.dto.BalanceDTO;
import pk.bm.pasir_malina_bartlomiej.dto.TransactionDTO;
import pk.bm.pasir_malina_bartlomiej.model.Transaction;
import pk.bm.pasir_malina_bartlomiej.model.TransactionType;
import pk.bm.pasir_malina_bartlomiej.model.User;
import pk.bm.pasir_malina_bartlomiej.repository.TransactionRepository;
import pk.bm.pasir_malina_bartlomiej.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public List<Transaction> getAllTransactions() {
        User user = getCurrentUser();
        return transactionRepository.findByUser(user);
    }

    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID: " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        return transaction;
    }

    public Transaction createTransaction(TransactionDTO dto) {
        return createTransactionForUser(dto, userService.getAuthenticatedUser());
    }

    /**
     * Tworzy transakcję dla wskazanego użytkownika (używane przez GroupTransactionService
     * do aktualizacji bilansu przy dodaniu wydatku grupowego).
     */
    public Transaction createTransactionForUser(TransactionDTO dto, User user) {
        Transaction transaction = new Transaction();
        transaction.setAmount(dto.getAmount());
        transaction.setType(TransactionType.valueOf(dto.getType()));
        transaction.setTags(dto.getTags());
        transaction.setNotes(dto.getNotes());
        transaction.setUser(user);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Long id, TransactionDTO dto) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        transaction.setAmount(dto.getAmount());
        transaction.setType(TransactionType.valueOf(dto.getType()));
        transaction.setTags(dto.getTags());
        transaction.setNotes(dto.getNotes());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz uprawnień do usunięcia tej transakcji");
        }
        transactionRepository.delete(transaction);
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Uzytkownik nie jest uwierzytelniony");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono zalogowanego uzytkownika: " + email));
    }

    public BalanceDTO getUserBalance(User user, Double days) {
        List<Transaction> transactions;

        if (days != null && days > 0) {
            long secondsToSubtract = Math.round(days * 24 * 60 * 60);
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(secondsToSubtract);
            transactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, threshold);
        } else {
            transactions = transactionRepository.findByUser(user);
        }

        double income = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        return new BalanceDTO(income, expense, income - expense);
    }
}
