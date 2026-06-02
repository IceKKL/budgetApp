package pk.bm.pasir_malina_bartlomiej.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bm.pasir_malina_bartlomiej.dto.GroupDTO;
import pk.bm.pasir_malina_bartlomiej.model.Group;
import pk.bm.pasir_malina_bartlomiej.model.Membership;
import pk.bm.pasir_malina_bartlomiej.model.User;
import pk.bm.pasir_malina_bartlomiej.repository.DebtRepository;
import pk.bm.pasir_malina_bartlomiej.repository.GroupRepository;
import pk.bm.pasir_malina_bartlomiej.repository.MembershipRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    @Transactional
    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser(); //get currently logged user

        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);

        Group savedGroup = groupRepository.save(group);

        Membership membership = new Membership();
        membership.setUser(owner);
        membership.setGroup(savedGroup);

        membershipRepository.save(membership);

        return savedGroup;
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć grupy. Grupa o ID " + id + " nie istnieje."));

        User currentUser = currentUserService.getCurrentUser();
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może ją usunąć.");
        }

        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }
}