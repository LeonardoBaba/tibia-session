package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class PartyTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fromPlayer;
    private String toPlayer;
    private long amount;

    public PartyTransfer(AdjustmentDTO adjustment) {
        this.fromPlayer = adjustment.from();
        this.toPlayer = adjustment.to();
        this.amount = adjustment.amount();
    }
}
