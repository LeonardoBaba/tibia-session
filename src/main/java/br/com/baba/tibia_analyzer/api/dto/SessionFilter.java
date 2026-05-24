package br.com.baba.tibia_analyzer.api.dto;

import java.time.LocalDateTime;

public record SessionFilter(
        String name,
        String player,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String ownerDiscordId
) {}
