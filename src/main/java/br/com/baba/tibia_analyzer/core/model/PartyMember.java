package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.discord.dto.PlayerDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
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
        this.name=playerDTO.getName();
        this.loot=playerDTO.getLoot();
        this.supplies=playerDTO.getSupplies();
        this.balance=playerDTO.getBalance();
        this.damage=playerDTO.getDamage();
        this.healing=playerDTO.getHealing();
    }
}
