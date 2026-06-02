package pk.bm.pasir_malina_bartlomiej.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.bm.pasir_malina_bartlomiej.dto.DebtDTO;
import pk.bm.pasir_malina_bartlomiej.model.Debt;
import pk.bm.pasir_malina_bartlomiej.service.DebtService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DebtGraphQLController {

    private final DebtService debtService;

    @QueryMapping
    public List<Debt> groupDebts(@Argument Long groupId) {
        return debtService.getGroupDebts(groupId);
    }

    @MutationMapping
    public Debt createDebt(@Valid @Argument DebtDTO debtDTO) {
        return debtService.createDebt(debtDTO);
    }

    @MutationMapping
    public Boolean deleteDebt(@Argument Long debtId) {
        debtService.deleteDebt(debtId);
        return true;
    }

    @MutationMapping
    public Debt markDebtAsPaid(@Argument Long debtId) {
        return debtService.markDebtAsPaid(debtId);
    }

    @MutationMapping
    public Debt confirmDebtPayment(@Argument Long debtId) {
        return debtService.confirmDebtPayment(debtId);
    }
}