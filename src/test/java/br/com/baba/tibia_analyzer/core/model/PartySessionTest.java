package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerStatDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PartySessionTest {

    @Test
    void shouldCreatePartySessionFromDTO() {
        // Arrange
        PlayerDTO player = new PlayerDTO("Player1", 100, 50, 50, 1000, 500);
        PartyHuntAnalyzerDTO dto = new PartyHuntAnalyzerDTO(
                "10:00", "11:00", "01:00h", 100, 50, 50, List.of(player)
        );
        SessionResultDTO result = new SessionResultDTO(
                1, 50, 50, 100, "01:00h",
                List.of(new PlayerStatDTO("Player1", 1000, 100.0)),
                List.of(new PlayerStatDTO("Player1", 500, 100.0)),
                List.of(new AdjustmentDTO("Player1", "Player2", 25))
        );
        String rawInput = "Raw Input String";

        // Act
        PartySession session = new PartySession(dto, result, rawInput);

        // Assert
        Assertions.assertEquals("10:00", session.getStartTime());
        Assertions.assertEquals("11:00", session.getEndTime());
        Assertions.assertEquals("01:00h", session.getSessionDuration());
        Assertions.assertEquals(100, session.getLoot());
        Assertions.assertEquals(50, session.getSupplies());
        Assertions.assertEquals(50, session.getBalance());
        Assertions.assertEquals(rawInput, session.getInputSession());
        Assertions.assertNotNull(session.getProcessDate());

        Assertions.assertEquals(1, session.getPartyMembers().size());
        Assertions.assertEquals("Player1", session.getPartyMembers().get(0).getName());

        Assertions.assertEquals(1, session.getPartyTransfers().size());
        PartyTransfer transfer = session.getPartyTransfers().get(0);
        Assertions.assertEquals("Player1", transfer.getFromPlayer());
        Assertions.assertEquals("Player2", transfer.getToPlayer());
        Assertions.assertEquals(25, transfer.getAmount());
    }
}
