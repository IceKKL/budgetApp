package pk.bm.pasir_malina_bartlomiej.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "debts")
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    private String title; // Transaction title or description

    public String getTitle() { return title != null ? title : "Brak opisu"; }

    private boolean paidByDebtor = false;
    private boolean confirmedByCreditor = false;

    @ManyToOne
    @JoinColumn(name = "debtor_id")
    private User debtor; // User who owes the money

    @ManyToOne
    @JoinColumn(name = "creditor_id")
    private User creditor; // User who is owed the money

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group; // Group to which this debt belongs
}