package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.discord.dto.PartyHuntAnalyzerDTO;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Data
public class PartySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String startTime;
    private String endTime;
    private String sessionDuration;
    private long loot;
    private long supplies;
    private long balance;
    private String inputSession;
    private String processedMessage;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "party_session_id")
    private List<PartyMember> partyMembers = new ArrayList<>();


    public PartySession(PartyHuntAnalyzerDTO analyzerDTO, String inputSession) {
        this.startTime = analyzerDTO.getStartTime();
        this.endTime = analyzerDTO.getEndTime();
        this.sessionDuration = analyzerDTO.getSessionDuration();
        this.loot = analyzerDTO.getLoot();
        this.supplies = analyzerDTO.getSupplies();
        this.balance = analyzerDTO.getBalance();
        this.inputSession = inputSession;
        this.processedMessage = analyzerDTO.getProcessedMessage();
        analyzerDTO.getPlayers().forEach(player -> {
            this.partyMembers.add(new PartyMember(player));
        });
    }
}
