package br.com.baba.tibia_analyzer.discord.dto;

public record AdjustmentDTO(
        String from,
        String to,
        long amount
) {}
