package br.com.baba.tibia_analyzer.discord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdjustmentDTO {
    private String from;
    private String to;
    private long amount;
}
