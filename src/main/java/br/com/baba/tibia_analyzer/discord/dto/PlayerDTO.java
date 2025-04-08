package br.com.baba.tibia_analyzer.discord.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerDTO {
    private String name;
    private long loot;
    private long supplies;
    private long balance;
    private long damage;
    private long healing;
}
