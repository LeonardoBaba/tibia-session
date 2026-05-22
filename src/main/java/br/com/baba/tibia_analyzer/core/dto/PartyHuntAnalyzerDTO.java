package br.com.baba.tibia_analyzer.core.dto;

import java.util.List;

public record PartyHuntAnalyzerDTO(
        String startTime,
        String endTime,
        String sessionDuration,
        long loot,
        long supplies,
        long balance,
        List<PlayerDTO> players
) {}
