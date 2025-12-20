package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PartyHuntSplitterTest {

    private final PartyHuntSplitter splitter = new PartyHuntSplitter();

    @Test
    void shouldCalculateSplitCorrectly() {
        // Arrange
        // Cenário: Lucro total 1000. Knight tem 1000 de saldo, Druid tem 0.
        // Cada um deve ficar com 500. Knight deve transferir 500 para o Druid.
        PlayerDTO knight = new PlayerDTO("Knight", 1500, 500, 1000, 0, 0);
        PlayerDTO druid = new PlayerDTO("Druid", 500, 500, 0, 0, 0);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "1h", 2000, 1000, 1000,
                List.of(knight, druid), null
        );

        // Act
        PartyHuntAnalyzerDTO resultDTO = splitter.split(inputDTO);

        // Assert
        String processedMessage = resultDTO.processedMessage();
        Assertions.assertNotNull(processedMessage);

        // Verifica totais
        Assertions.assertTrue(processedMessage.contains("Total Loot: 2000"));
        Assertions.assertTrue(processedMessage.contains("Total Supplies: 1000"));
        Assertions.assertTrue(processedMessage.contains("Total: 1000"));

        // Verifica transferência
        // O Knight (saldo 1000) excede a média (500) em 500.
        // O Druid (saldo 0) está abaixo da média (500) em 500.
        // Knight transfere 500 para Druid.

        Assertions.assertTrue(processedMessage.contains("Knight"));
        Assertions.assertTrue(processedMessage.contains("transfer 500 to Druid"));
    }

    @Test
    void shouldCalculateSplitWhenEveryoneHasLoss() {
        // Arrange
        // Cenário: Todos com prejuízo (Waste).
        // Knight: Loot 0, Supplies 1000, Balance -1000.
        // Druid: Loot 0, Supplies 2000, Balance -2000.
        // Total Balance: -3000. Média: -1500.
        // Knight perdeu 1000 (melhor que a média de -1500), então ele "lucrou" 500 em relação à média.
        // Druid perdeu 2000 (pior que a média de -1500), então ele deve receber 500 para aliviar o prejuízo.
        
        PlayerDTO knight = new PlayerDTO("Knight", 0, 1000, -1000, 0, 0);
        PlayerDTO druid = new PlayerDTO("Druid", 0, 2000, -2000, 0, 0);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "1h", 0, 3000, -3000,
                List.of(knight, druid), null
        );

        // Act
        PartyHuntAnalyzerDTO resultDTO = splitter.split(inputDTO);

        // Assert
        String processedMessage = resultDTO.processedMessage();
        Assertions.assertNotNull(processedMessage);

        Assertions.assertTrue(processedMessage.contains("Total Loot: 0"));
        Assertions.assertTrue(processedMessage.contains("Total Supplies: 3000"));
        Assertions.assertTrue(processedMessage.contains("Total: -3000"));

        // Knight transfere 500 para o Druid para que ambos fiquem com -1500 de saldo final.
        Assertions.assertTrue(processedMessage.contains("Knight"));
        Assertions.assertTrue(processedMessage.contains("transfer 500 to Druid"));
    }
}