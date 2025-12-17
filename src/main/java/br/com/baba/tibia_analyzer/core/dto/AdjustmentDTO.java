package br.com.baba.tibia_analyzer.core.dto;

public record AdjustmentDTO(
        String from,
        String to,
        long amount
) {}
