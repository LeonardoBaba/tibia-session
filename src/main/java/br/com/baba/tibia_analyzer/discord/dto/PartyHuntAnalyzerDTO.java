package br.com.baba.tibia_analyzer.discord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartyHuntAnalyzerDTO {
    private String startTime;
    private String endTime;
    private String sessionDuration;
    private long loot;
    private long supplies;
    private long balance;
    private List<PlayerDTO> players = new ArrayList<>();
    private String processedMessage;

    public void addPlayer(PlayerDTO player) {
        this.players.add(player);
    }
}
