package br.com.baba.tibia_analyzer.discord.dto;

import java.util.List;

public record PartyHuntAnalyzerDTO(
        String startTime,
        String endTime,
        String sessionDuration,
        long loot,
        long supplies,
        long balance,
        List<PlayerDTO> players,
        String processedMessage
) {
    public PartyHuntAnalyzerDTO(String startTime, String endTime, String sessionDuration, long loot, long supplies, long balance, List<PlayerDTO> players) {
        this(startTime, endTime, sessionDuration, loot, supplies, balance, players, null);
    }

    public PartyHuntAnalyzerDTO(PartyHuntAnalyzerDTO original, String processedMessage) {
        this(original.startTime(), original.endTime(), original.sessionDuration(), original.loot(), original.supplies(), original.balance(), original.players(), processedMessage);
    }
}
