package br.com.baba.tibia_analyzer.discord.embed;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerStatDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

class PartyHuntEmbedFactoryTest {

    private final PartyHuntEmbedFactory factory = new PartyHuntEmbedFactory();

    @Test
    void shouldBuildEmbedFromResult() {
        // Arrange
        SessionResultDTO result = new SessionResultDTO(
                2, 1_178_905, 589_452, 2_034_616, "01:05h",
                List.of(new PlayerStatDTO("Hikeppo", 100, 56.24),
                        new PlayerStatDTO("Volta Mcfish", 78, 43.76)),
                List.of(new PlayerStatDTO("Volta Mcfish", 200, 65.95),
                        new PlayerStatDTO("Hikeppo", 103, 34.05)),
                List.of(new AdjustmentDTO("Hikeppo", "Volta Mcfish", 507442))
        );

        // Act
        MessageEmbed embed = factory.build(result);

        // Assert
        Assertions.assertEquals("Party Hunt Session – 2 members", embed.getTitle());

        String description = embed.getDescription();
        Assertions.assertNotNull(description);
        Assertions.assertTrue(description.contains("1,178,905"));
        Assertions.assertTrue(description.contains("589,452"));
        Assertions.assertTrue(description.contains("2,034,616"));

        MessageEmbed.Field damage = findField(embed, "Damage");
        Assertions.assertTrue(damage.getValue().contains("Hikeppo (56.24%)"));

        MessageEmbed.Field transfers = findField(embed, "Transfers for Hikeppo");
        Assertions.assertTrue(transfers.getValue().contains("transfer 507442 to Volta Mcfish"));

        Assertions.assertNotNull(embed.getFooter());
        Assertions.assertEquals("01:05h hunt", embed.getFooter().getText());

        // Lucro positivo: faixa verde.
        Assertions.assertEquals(new Color(0x2ECC71), embed.getColor());
    }

    @Test
    void shouldUseRedColorForWasteSession() {
        // Arrange
        SessionResultDTO result = new SessionResultDTO(
                2, -500_000, -250_000, 0, "00:45h",
                List.of(new PlayerStatDTO("Knight", 10, 100.0)),
                List.of(new PlayerStatDTO("Druid", 5, 100.0)),
                List.of()
        );

        // Act
        MessageEmbed embed = factory.build(result);

        // Assert
        Assertions.assertEquals(new Color(0xE74C3C), embed.getColor());
    }

    @Test
    void shouldUseNeutralColorWhenBalanceIsZero() {
        // Arrange
        SessionResultDTO result = new SessionResultDTO(
                2, 0, 0, 0, "00:45h",
                List.of(new PlayerStatDTO("Knight", 10, 100.0)),
                List.of(new PlayerStatDTO("Druid", 5, 100.0)),
                List.of()
        );

        // Act
        MessageEmbed embed = factory.build(result);

        // Assert
        Assertions.assertEquals(new Color(0x95A5A6), embed.getColor());
    }

    @Test
    void shouldRenderPlaceholderWhenThereAreNoTransfers() {
        // Arrange
        SessionResultDTO result = new SessionResultDTO(
                1, 0, 0, 0, "00:30h",
                List.of(new PlayerStatDTO("Solo", 10, 100.0)),
                List.of(new PlayerStatDTO("Solo", 5, 100.0)),
                List.of()
        );

        // Act
        MessageEmbed embed = factory.build(result);

        // Assert
        MessageEmbed.Field transfers = findField(embed, "Transfers");
        Assertions.assertTrue(transfers.getValue().contains("Nothing to transfer"));
    }

    private MessageEmbed.Field findField(MessageEmbed embed, String name) {
        return embed.getFields().stream()
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + name));
    }
}
