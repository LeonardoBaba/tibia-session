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
        this.startTime = analyzerDTO.startTime();
        this.endTime = analyzerDTO.endTime();
        this.sessionDuration = analyzerDTO.sessionDuration();
        this.loot = analyzerDTO.loot();
        this.supplies = analyzerDTO.supplies();
        this.balance = analyzerDTO.balance();
        this.inputSession = inputSession;
        this.processedMessage = analyzerDTO.processedMessage();
        analyzerDTO.players().forEach(player -> {
            this.partyMembers.add(new PartyMember(player));
        });
    }
}
