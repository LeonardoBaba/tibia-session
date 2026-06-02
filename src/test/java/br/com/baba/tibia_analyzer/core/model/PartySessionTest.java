package br.com.baba.tibia_analyzer.core.model;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerStatDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class PartySessionTest {

    @Test
    void shouldCreatePartySessionFromDTO() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 11, 0, 0);
        PlayerDTO player = new PlayerDTO("Player1", 100, 50, 50, 1000, 500);
        PartyHuntAnalyzerDTO dto = new PartyHuntAnalyzerDTO(
                start, end, "01:00h", 100, 50, 50, List.of(player)
        );
        SessionResultDTO result = new SessionResultDTO(
                1, 50, 50, 100, "01:00h",
                List.of(new PlayerStatDTO("Player1", 1000, 100.0)),
                List.of(new PlayerStatDTO("Player1", 500, 100.0)),
                List.of(new AdjustmentDTO("Player1", "Player2", 25))
        );
        String rawInput = "Raw Input String";

        PartySession session = new PartySession(dto, result, rawInput, "Cobra Bastion", "Boss died -1", "123456");

        Assertions.assertEquals("Cobra Bastion", session.getName());
        Assertions.assertEquals("Boss died -1", session.getComment());
        Assertions.assertEquals("123456", session.getOwnerDiscordId());
        Assertions.assertEquals(start, session.getStartTime());
        Assertions.assertEquals(end, session.getEndTime());
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

    @Test
    void shouldAllowNullOptionalFields() {
        PartyHuntAnalyzerDTO dto = new PartyHuntAnalyzerDTO(
                null, null, "00:00h", 0, 0, 0, List.of()
        );
        SessionResultDTO result = new SessionResultDTO(
                0, 0, 0, 0, "00:00h", List.of(), List.of(), List.of()
        );

        PartySession session = new PartySession(dto, result, "", null, null, null);

        Assertions.assertNull(session.getName());
        Assertions.assertNull(session.getComment());
        Assertions.assertNull(session.getOwnerDiscordId());
        Assertions.assertNull(session.getStartTime());
        Assertions.assertNull(session.getEndTime());
    }
}
