package br.com.baba.tibia_analyzer.discord.dto;

import lombok.Getter;

@Getter
public class AdjustmentDTO {
    String from;
    String to;
    long amount;

    public AdjustmentDTO(String from, String to, long amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }
}
