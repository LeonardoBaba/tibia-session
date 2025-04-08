package br.com.baba.tibia_analyzer.discord.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerBalanceDTO {
    String name;
    long balance;

    public PlayerBalanceDTO(String name, long balance) {
        this.name = name;
        this.balance = balance;
    }
}
