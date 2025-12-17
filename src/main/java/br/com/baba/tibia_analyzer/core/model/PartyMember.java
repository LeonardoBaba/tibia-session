package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.discord.dto.PlayerDTO;
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
public class PartyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private long loot;
    private long supplies;
    private long balance;
    private long damage;
    private long healing;

    public PartyMember(PlayerDTO playerDTO) {
        this.name=playerDTO.name();
        this.loot=playerDTO.loot();
        this.supplies=playerDTO.supplies();
        this.balance=playerDTO.balance();
        this.damage=playerDTO.damage();
        this.healing=playerDTO.healing();
    }
}
