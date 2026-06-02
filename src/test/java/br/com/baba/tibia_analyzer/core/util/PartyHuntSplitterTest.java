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
        PlayerDTO knight = new PlayerDTO("Knight", 1500, 500, 1000, 0, 0);
        PlayerDTO druid = new PlayerDTO("Druid", 500, 500, 0, 0, 0);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                null, null, "01:00h", 2000, 1000, 1000, List.of(knight, druid)
        );

        SessionResultDTO result = splitter.split(inputDTO);

        Assertions.assertEquals(2, result.memberCount());
        Assertions.assertEquals(1000, result.balance());
        Assertions.assertEquals(500, result.individualBalance());
        Assertions.assertEquals(2000, result.lootPerHour());

        Assertions.assertEquals(1, result.transfers().size());
        AdjustmentDTO transfer = result.transfers().get(0);
        Assertions.assertEquals("Knight", transfer.from());
        Assertions.assertEquals("Druid", transfer.to());
        Assertions.assertEquals(500, transfer.amount());
    }

    @Test
    void shouldCalculateSplitWhenEveryoneHasLoss() {
        PlayerDTO knight = new PlayerDTO("Knight", 0, 1000, -1000, 0, 0);
        PlayerDTO druid = new PlayerDTO("Druid", 0, 2000, -2000, 0, 0);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                null, null, "01:00h", 0, 3000, -3000, List.of(knight, druid)
        );

        SessionResultDTO result = splitter.split(inputDTO);

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
        PlayerDTO playerA = new PlayerDTO("A", 1000, 0, 1000, 7500, 2000);
        PlayerDTO playerB = new PlayerDTO("B", 0, 0, 0, 2500, 8000);

        PartyHuntAnalyzerDTO inputDTO = new PartyHuntAnalyzerDTO(
                null, null, "02:00h", 1000, 0, 1000, List.of(playerA, playerB)
        );

        SessionResultDTO result = splitter.split(inputDTO);

        Assertions.assertEquals(500, result.lootPerHour());

        List<PlayerStatDTO> damage = result.damage();
        Assertions.assertEquals("A", damage.get(0).name());
        Assertions.assertEquals(75.0, damage.get(0).percentage(), 0.001);
        Assertions.assertEquals("B", damage.get(1).name());
        Assertions.assertEquals(25.0, damage.get(1).percentage(), 0.001);

        List<PlayerStatDTO> healing = result.healing();
        Assertions.assertEquals("B", healing.get(0).name());
        Assertions.assertEquals(80.0, healing.get(0).percentage(), 0.001);
        Assertions.assertEquals("A", healing.get(1).name());
        Assertions.assertEquals(20.0, healing.get(1).percentage(), 0.001);
    }
}
