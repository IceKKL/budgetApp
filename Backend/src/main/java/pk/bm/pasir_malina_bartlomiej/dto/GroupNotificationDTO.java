package pk.bm.pasir_malina_bartlomiej.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupNotificationDTO {
    private String type;           // Wartość: "GROUP_EXPENSE_ADDED"
    private Long groupId;          // ID grupy, w której dodano wydatek [cite: 291]
    private String groupName;      // Nazwa grupy [cite: 294]
    private String title;          // Tytuł wydatku [cite: 295]
    private double amount;         // Całkowita kwota transakcji [cite: 296]
    private double userShare;      // Część przypadająca na dany podmiot [cite: 297]
    private String createdByEmail; // Email osoby dodającej [cite: 298]
    private String message;        // Tekst powiadomienia wyświetlany użytkownikowi [cite: 287, 299]
}