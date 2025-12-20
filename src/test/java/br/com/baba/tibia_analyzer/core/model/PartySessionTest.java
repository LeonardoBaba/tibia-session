package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PartySessionTest {

    @Test
    void shouldCreatePartySessionFromDTO() {
        // Arrange
        PlayerDTO player = new PlayerDTO("Player1", 100, 50, 50, 1000, 500);
        PartyHuntAnalyzerDTO dto = new PartyHuntAnalyzerDTO(
                "10:00", "11:00", "01:00", 100, 50, 50,
                List.of(player), "Processed Message"
        );
        String rawInput = "Raw Input String";

        // Act
        PartySession session = new PartySession(dto, rawInput);

        // Assert
        Assertions.assertEquals("10:00", session.getStartTime());
        Assertions.assertEquals("11:00", session.getEndTime());
        Assertions.assertEquals("01:00", session.getSessionDuration());
        Assertions.assertEquals(100, session.getLoot());
        Assertions.assertEquals(50, session.getSupplies());
        Assertions.assertEquals(50, session.getBalance());
        Assertions.assertEquals(rawInput, session.getInputSession());
        Assertions.assertEquals("Processed Message", session.getProcessedMessage());
        Assertions.assertNotNull(session.getProcessDate());
        Assertions.assertEquals(1, session.getPartyMembers().size());
        Assertions.assertEquals("Player1", session.getPartyMembers().get(0).getName());
    }
}