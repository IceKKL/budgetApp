package pk.bm.pasir_malina_bartlomiej.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.bm.pasir_malina_bartlomiej.model.Group;
import pk.bm.pasir_malina_bartlomiej.model.User;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByMemberships_User(User user);
}