package br.com.baba.tibia_analyzer.core.dto;

public record PlayerDTO(
        String name,
        long loot,
        long supplies,
        long balance,
        long damage,
        long healing
) {}
