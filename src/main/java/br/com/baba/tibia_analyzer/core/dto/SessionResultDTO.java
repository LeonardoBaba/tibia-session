package br.com.baba.tibia_analyzer.core.dto;

import java.util.List;

public record SessionResultDTO(
        int memberCount,
        long balance,
        long individualBalance,
        long lootPerHour,
        String sessionDuration,
        List<PlayerStatDTO> damage,
        List<PlayerStatDTO> healing,
        List<AdjustmentDTO> transfers
) {}
