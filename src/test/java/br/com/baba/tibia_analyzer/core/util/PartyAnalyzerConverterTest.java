package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class PartyAnalyzerConverterTest {

    private final PartyAnalyzerConverter converter = new PartyAnalyzerConverter();

    @Test
    void shouldConvertTextFileToAnalyzerDTO() throws IOException {
        // Arrange
        Path path = Paths.get("src/test/resources/analyzer.txt");
        String input = Files.readString(path);

        // Act
        PartyHuntAnalyzerDTO result = converter.getAnalyzer(input);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("2025-12-16, 12:13:38", result.startTime());
        Assertions.assertEquals("2025-12-16, 13:43:29", result.endTime());
        Assertions.assertEquals("01:29h", result.sessionDuration());
        Assertions.assertEquals(3_103_516L, result.loot());
        Assertions.assertEquals(1_450_492L, result.supplies());
        Assertions.assertEquals(1_653_024L, result.balance());

        List<PlayerDTO> players = result.players();
        Assertions.assertEquals(2, players.size());

        PlayerDTO player1 = players.get(0);
        Assertions.assertEquals("Hikeppo", player1.name());
        Assertions.assertEquals(2_669_816L, player1.loot());
        Assertions.assertEquals(1_112_890L, player1.supplies());
        Assertions.assertEquals(1_556_926L, player1.balance());

        PlayerDTO player2 = players.get(1);
        Assertions.assertEquals("Volta Mcfish", player2.name());
        Assertions.assertEquals(433_700L, player2.loot());
        Assertions.assertEquals(337_602L, player2.supplies());
        Assertions.assertEquals(96_098L, player2.balance());
    }
}