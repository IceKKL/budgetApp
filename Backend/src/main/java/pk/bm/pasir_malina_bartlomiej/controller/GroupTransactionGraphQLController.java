package pk.bm.pasir_malina_bartlomiej.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.bm.pasir_malina_bartlomiej.dto.GroupTransactionDTO;
import pk.bm.pasir_malina_bartlomiej.model.User;
import pk.bm.pasir_malina_bartlomiej.service.CurrentUserService;
import pk.bm.pasir_malina_bartlomiej.service.GroupTransactionService;

@Controller
@RequiredArgsConstructor
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;
    private final CurrentUserService currentUserService;

    @MutationMapping
    public Boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO) {
        User user = currentUserService.getCurrentUser();
        groupTransactionService.addGroupTransaction(groupTransactionDTO, user);
        return true;
    }
}