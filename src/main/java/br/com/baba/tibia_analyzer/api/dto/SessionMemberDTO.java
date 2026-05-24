package br.com.baba.tibia_analyzer.api.dto;

import java.util.UUID;

public record SessionMemberDTO(
        UUID id,
        String name,
        long loot,
        long supplies,
        long balance,
        long damage,
        long healing
) {}
