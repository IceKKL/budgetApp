package pk.bm.pasir_malina_bartlomiej.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "`groups`") // `group` is a reserved keyword in SQL, so we need to escape it
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; //Group name

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner; //Group owner (can invite and remove other users)

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships; // List of group memberships (users in the group)

    @Transient
    public Long getOwnerId() { return owner != null ? owner.getId() : null; }
}