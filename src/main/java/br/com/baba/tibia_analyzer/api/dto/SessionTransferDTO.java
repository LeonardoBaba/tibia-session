package br.com.baba.tibia_analyzer.api.dto;

import java.util.UUID;

public record SessionTransferDTO(
        UUID id,
        String fromPlayer,
        String toPlayer,
        long amount
) {}
