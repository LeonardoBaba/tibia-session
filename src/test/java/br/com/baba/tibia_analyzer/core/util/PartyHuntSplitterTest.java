package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerStatDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
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
                "start", "end", "01:00h", 2000, 1000, 1000, List.of(knight, druid)
        );

        // Act
        SessionResultDTO result = splitter.split(inputDTO);

        // Assert
        Assertions.assertEquals(2, result.memberCount());
        Assertions.assertEquals(1000, result.balance());
        Assertions.assertEquals(500, result.individualBalance());
        // Loot total 2000 dividido por 1h de sessão.
        Assertions.assertEquals(2000, result.lootPerHour());

        Assertions.assertEquals(1, result.transfers().size());
        AdjustmentDTO transfer = result.transfers().get(0);
        Assertions.assertEquals("Knight", transfer.from());
        Assertions.assertEquals("Druid", transfer.to());
        Assertions.assertEquals(500, transfer.amount());
    }

    @Test
    void shouldCalculateSplitWhenEveryoneHasLoss() {
        // Arrange
        // Cenário: Todos com prejuízo. Total Balance: -3000. Média: -1500.
        // Knight perdeu 1000 (acima da média), Druid perdeu 2000 (abaixo da média).
        // Knight transfere 500 para o Druid para ambos ficarem com -1500.
        PlayerDTO knight = new PlayerDTO("Knight", 0, 1000, -1000, 0, 0);
        PlayerDTO druid = new PlayerDTO("Druid", 0, 2000, -2000, 0, 0);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "01:00h", 0, 3000, -3000, List.of(knight, druid)
        );

        // Act
        SessionResultDTO result = splitter.split(inputDTO);

        // Assert
        Assertions.assertEquals(-3000, result.balance());
        Assertions.assertEquals(-1500, result.individualBalance());
        Assertions.assertEquals(0, result.lootPerHour());

        Assertions.assertEquals(1, result.transfers().size());
        AdjustmentDTO transfer = result.transfers().get(0);
        Assertions.assertEquals("Knight", transfer.from());
        Assertions.assertEquals("Druid", transfer.to());
        Assertions.assertEquals(500, transfer.amount());
    }

    @Test
    void shouldRankDamageAndHealingByPercentage() {
        // Arrange
        // Dano total 10000: A 75%, B 25%. Cura total 10000: A 20%, B 80%.
        PlayerDTO playerA = new PlayerDTO("A", 1000, 0, 1000, 7500, 2000);
        PlayerDTO playerB = new PlayerDTO("B", 0, 0, 0, 2500, 8000);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "02:00h", 1000, 0, 1000, List.of(playerA, playerB)
        );

        // Act
        SessionResultDTO result = splitter.split(inputDTO);

        // Assert
        // Loot total 1000 dividido por 2h de sessão.
        Assertions.assertEquals(500, result.lootPerHour());

        // Dano ordenado do maior para o menor.
        List<PlayerStatDTO> damage = result.damage();
        Assertions.assertEquals("A", damage.get(0).name());
        Assertions.assertEquals(75.0, damage.get(0).percentage(), 0.001);
        Assertions.assertEquals("B", damage.get(1).name());
        Assertions.assertEquals(25.0, damage.get(1).percentage(), 0.001);

        // Cura ordenada do maior para o menor.
        List<PlayerStatDTO> healing = result.healing();
        Assertions.assertEquals("B", healing.get(0).name());
        Assertions.assertEquals(80.0, healing.get(0).percentage(), 0.001);
        Assertions.assertEquals("A", healing.get(1).name());
        Assertions.assertEquals(20.0, healing.get(1).percentage(), 0.001);
    }
}
