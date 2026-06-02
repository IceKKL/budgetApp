package pk.bm.pasir_malina_bartlomiej.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bm.pasir_malina_bartlomiej.config.GroupNotificationHandler;
import pk.bm.pasir_malina_bartlomiej.dto.GroupNotificationDTO;
import pk.bm.pasir_malina_bartlomiej.dto.GroupTransactionDTO;
import pk.bm.pasir_malina_bartlomiej.dto.TransactionDTO;
import pk.bm.pasir_malina_bartlomiej.model.Debt;
import pk.bm.pasir_malina_bartlomiej.model.Group;
import pk.bm.pasir_malina_bartlomiej.model.Membership;
import pk.bm.pasir_malina_bartlomiej.model.User;
import pk.bm.pasir_malina_bartlomiej.repository.DebtRepository;
import pk.bm.pasir_malina_bartlomiej.repository.GroupRepository;
import pk.bm.pasir_malina_bartlomiej.repository.MembershipRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final TransactionService transactionService;
    private final GroupNotificationHandler groupNotificationHandler;

    @Transactional
    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);
        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma czlonkow, nie mozna dodac transakcji.");
        }

        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());

        // Aktualizacja bilansu użytkownika dodającego transakcję
        TransactionDTO balanceUpdate = new TransactionDTO();
        balanceUpdate.setAmount(transactionDTO.getAmount());
        balanceUpdate.setType(expense ? "EXPENSE" : "INCOME");
        balanceUpdate.setNotes("Transakcja grupowa: " + group.getName() + " - " + transactionDTO.getTitle());
        transactionService.createTransactionForUser(balanceUpdate, currentUser);

        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                // Zapis długu
                Debt debt = new Debt();
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);

                // Powiadomienie WebSocket — wysyłamy bezpośrednio przez handler
                String msgText = String.format(
                        "%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                        currentUser.getEmail(), transactionDTO.getTitle(),
                        group.getName(), amountPerUser);

                GroupNotificationDTO notification = new GroupNotificationDTO(
                        "GROUP_EXPENSE_ADDED",
                        group.getId(),
                        group.getName(),
                        transactionDTO.getTitle(),
                        transactionDTO.getAmount(),
                        amountPerUser,
                        currentUser.getEmail(),
                        msgText
                );

                groupNotificationHandler.sendNotification(otherUser.getEmail(), notification);
            }
        }
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {
        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }
        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();
        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException(
                    "Wszyscy wybrani uzytkownicy musza byc czlonkami grupy.");
        }
        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException(
                    "Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }
        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }
        return selectedMembers;
    }
}
