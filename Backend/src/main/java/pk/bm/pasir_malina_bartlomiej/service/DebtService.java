package pk.bm.pasir_malina_bartlomiej.service;

import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import pk.bm.pasir_malina_bartlomiej.dto.DebtDTO;
import pk.bm.pasir_malina_bartlomiej.dto.TransactionDTO;
import pk.bm.pasir_malina_bartlomiej.model.Debt;
import pk.bm.pasir_malina_bartlomiej.model.Group;
import pk.bm.pasir_malina_bartlomiej.model.User;
import pk.bm.pasir_malina_bartlomiej.repository.DebtRepository;
import pk.bm.pasir_malina_bartlomiej.repository.GroupRepository;
import pk.bm.pasir_malina_bartlomiej.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DebtService {

    private static final String NOT_FOUND_SUFFIX = " nie istnieje.";

    private final DebtRepository debtRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;
    private final TransactionService transactionService;

    public List<Debt> getGroupDebts(Long groupId) {
        membershipService.assertCurrentUserIsGroupMember(groupId);
        return debtRepository.findByGroupId(groupId);
    }

    @Transactional
    public Debt createDebt(DebtDTO debtDTO) {
        Group group = groupRepository.findById(debtDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Grupa o ID " + debtDTO.getGroupId() + NOT_FOUND_SUFFIX));

        User debtor = userRepository.findById(debtDTO.getDebtorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Dłużnik o ID " + debtDTO.getDebtorId() + NOT_FOUND_SUFFIX));

        User creditor = userRepository.findById(debtDTO.getCreditorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Wierzyciel o ID " + debtDTO.getCreditorId() + NOT_FOUND_SUFFIX));

        membershipService.assertCurrentUserIsGroupMember(group.getId());
        membershipService.assertUserIsGroupMember(group.getId(), debtor.getId());
        membershipService.assertUserIsGroupMember(group.getId(), creditor.getId());

        if (debtor.getId().equals(creditor.getId())) {
            throw new IllegalStateException("Dłużnik i wierzyciel muszą być różnymi użytkownikami.");
        }

        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(group, debtor, creditor, currentUser);

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(debtDTO.getAmount());
        debt.setTitle(debtDTO.getTitle());

        return debtRepository.save(debt);
    }

    @Transactional
    public void deleteDebt(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć długu. Dług o ID " + debtId + NOT_FOUND_SUFFIX));

        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(debt.getGroup(), debt.getDebtor(), debt.getCreditor(), currentUser);

        debtRepository.delete(debt);
    }

    private void assertCurrentUserCanManageDebt(Group group, User debtor, User creditor, User currentUser) {
        boolean isGroupOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean isDebtParticipant = debtor.getId().equals(currentUser.getId())
                || creditor.getId().equals(currentUser.getId());

        if (!isGroupOwner && !isDebtParticipant) {
            throw new AccessDeniedException(
                    "Tylko właściciel grupy albo uczestnik długu może wykonać te operacje.");
        }
    }

    @Transactional
    public Debt markDebtAsPaid(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();

        if (!debt.getDebtor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko dluznik moze oznaczyc dlug jako oplacony.");
        }

        debt.setPaidByDebtor(true);
        debt.setConfirmedByCreditor(false);

        TransactionDTO debtorUpdate = new TransactionDTO();
        debtorUpdate.setAmount(debt.getAmount());
        debtorUpdate.setType("EXPENSE");
        debtorUpdate.setNotes("Spłata długu: " + debt.getTitle() + " (grupa: " + debt.getGroup().getName() + ")");
        transactionService.createTransactionForUser(debtorUpdate, debt.getDebtor());

        return debtRepository.save(debt);
    }

    @Transactional
    public Debt confirmDebtPayment(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();

        if (!debt.getCreditor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko wierzyciel moze potwierdzic splate dlugu.");
        }

        if (!debt.isPaidByDebtor()) {
            throw new IllegalStateException("Dlug musi zostac najpierw oznaczony jako oplacony przez dluznika.");
        }

        debt.setConfirmedByCreditor(true);

        TransactionDTO creditorUpdate = new TransactionDTO();
        creditorUpdate.setAmount(debt.getAmount());
        creditorUpdate.setType("INCOME");
        creditorUpdate.setNotes("Potwierdzona spłata: " + debt.getTitle() + " (grupa: " + debt.getGroup().getName() + ")");
        transactionService.createTransactionForUser(creditorUpdate, debt.getCreditor());

        return debtRepository.save(debt);
    }

    private Debt getDebtForCurrentGroupMember(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono dlugu o ID " + debtId + "."));

        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        return debt;
    }
}