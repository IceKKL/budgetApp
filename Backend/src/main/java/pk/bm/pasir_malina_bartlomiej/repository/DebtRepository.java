package pk.bm.pasir_malina_bartlomiej.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.bm.pasir_malina_bartlomiej.model.Debt;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);
}