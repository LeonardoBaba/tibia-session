package br.com.baba.tibia_analyzer.discord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {
    private String name;
    private long loot;
    private long supplies;
    private long balance;
    private long damage;
    private long healing;
}
