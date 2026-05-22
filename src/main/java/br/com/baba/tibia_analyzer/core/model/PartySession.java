package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
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
    private Date processDate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "party_session_id")
    private List<PartyMember> partyMembers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "party_session_id")
    private List<PartyTransfer> partyTransfers = new ArrayList<>();

    public PartySession(PartyHuntAnalyzerDTO analyzerDTO, SessionResultDTO result, String inputSession) {
        this.startTime = analyzerDTO.startTime();
        this.endTime = analyzerDTO.endTime();
        this.sessionDuration = analyzerDTO.sessionDuration();
        this.loot = analyzerDTO.loot();
        this.supplies = analyzerDTO.supplies();
        this.balance = analyzerDTO.balance();
        this.inputSession = inputSession;
        this.processDate = new Date();
        analyzerDTO.players().forEach(player -> this.partyMembers.add(new PartyMember(player)));
        result.transfers().forEach(transfer -> this.partyTransfers.add(new PartyTransfer(transfer)));
    }
}
