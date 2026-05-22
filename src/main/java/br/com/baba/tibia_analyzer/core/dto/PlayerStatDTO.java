package br.com.baba.tibia_analyzer.core.dto;

public record PlayerStatDTO(
        String name,
        long value,
        double percentage
) {}
