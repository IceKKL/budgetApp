package pk.bm.pasir_malina_bartlomiej.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.bm.pasir_malina_bartlomiej.model.Membership;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupId(Long groupId);
}