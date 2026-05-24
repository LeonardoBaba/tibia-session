package br.com.baba.tibia_analyzer.api.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SessionDetailDTO(
        UUID id,
        String name,
        String comment,
        String ownerDiscordId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String sessionDuration,
        long loot,
        long supplies,
        long balance,
        Instant processDate,
        List<SessionMemberDTO> members,
        List<SessionTransferDTO> transfers
) {}
