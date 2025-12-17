package br.com.baba.tibia_analyzer.discord.dto;

public record PlayerDTO(
        String name,
        long loot,
        long supplies,
        long balance,
        long damage,
        long healing
) {}
