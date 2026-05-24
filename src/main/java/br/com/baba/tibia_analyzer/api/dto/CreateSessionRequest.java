package br.com.baba.tibia_analyzer.api.dto;

public record CreateSessionRequest(
        String input,
        String name,
        String comment,
        String ownerDiscordId
) {}
