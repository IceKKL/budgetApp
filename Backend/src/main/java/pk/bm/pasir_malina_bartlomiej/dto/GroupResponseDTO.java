package pk.bm.pasir_malina_bartlomiej.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponseDTO {

    private Long id;
    private String name;
    private Long ownerId;
}