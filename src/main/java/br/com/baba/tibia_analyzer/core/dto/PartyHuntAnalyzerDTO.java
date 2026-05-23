package br.com.baba.tibia_analyzer.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PartyHuntAnalyzerDTO(
        LocalDateTime startTime,
        LocalDateTime endTime,
        String sessionDuration,
        long loot,
        long supplies,
        long balance,
        List<PlayerDTO> players
) {}
