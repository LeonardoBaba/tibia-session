package br.com.baba.tibia_analyzer.api.dto;

public record UserDTO(
        String discordId,
        String username,
        String avatarUrl
) {}
