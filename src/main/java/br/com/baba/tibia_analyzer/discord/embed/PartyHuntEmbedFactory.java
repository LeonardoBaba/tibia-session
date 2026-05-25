package br.com.baba.tibia_analyzer.discord.embed;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerStatDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PartyHuntEmbedFactory {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final Color PROFIT_COLOR = new Color(0x2ECC71);
    private static final Color WASTE_COLOR = new Color(0xE74C3C);
    private static final Color NEUTRAL_COLOR = new Color(0x95A5A6);

    private final String frontendUrl;

    public PartyHuntEmbedFactory(@Value("${analyzer.frontend.url}") String frontendUrl) {
        // Remove barra final pra evitar "//hunts/..." quando concatenar.
        this.frontendUrl = frontendUrl == null ? null : frontendUrl.replaceAll("/+$", "");
    }

    public MessageEmbed build(SessionResultDTO result, UUID sessionId) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setColor(colorFor(result.balance()));
        embed.setTitle(
                "Party Hunt Session – " + result.memberCount() + " members",
                buildSessionUrl(sessionId));
        embed.setDescription(
                "**Balance:** " + format(result.balance()) + "\n" +
                "**Individual balance:** " + format(result.individualBalance()) + "\n" +
                "**Loot per hour:** " + format(result.lootPerHour()));

        embed.addField("Damage", formatRanking(result.damage()), true);
        embed.addField("Healing", formatRanking(result.healing()), true);

        appendTransfers(embed, result.transfers());

        if (sessionId != null && frontendUrl != null) {
            embed.addField("Open in browser", buildSessionUrl(sessionId), false);
        }

        embed.setFooter(result.sessionDuration() + " hunt");
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private String buildSessionUrl(UUID sessionId) {
        if (sessionId == null || frontendUrl == null || frontendUrl.isBlank()) {
            return null;
        }
        return frontendUrl + "/hunts/" + sessionId;
    }

    private String formatRanking(List<PlayerStatDTO> stats) {
        if (stats.isEmpty()) {
            return "-";
        }
        return stats.stream()
                .map(stat -> "› " + stat.name()
                        + " (" + String.format(Locale.US, "%.2f", stat.percentage()) + "%)")
                .collect(Collectors.joining("\n"));
    }

    private void appendTransfers(EmbedBuilder embed, List<AdjustmentDTO> transfers) {
        if (transfers.isEmpty()) {
            embed.addField("Transfers", "Nothing to transfer — everyone is even.", false);
            return;
        }

        Map<String, List<AdjustmentDTO>> byPayer = new LinkedHashMap<>();
        for (AdjustmentDTO transfer : transfers) {
            byPayer.computeIfAbsent(transfer.from(), key -> new ArrayList<>()).add(transfer);
        }

        byPayer.forEach((payer, payerTransfers) -> {
            String body = payerTransfers.stream()
                    .map(transfer -> "```\ntransfer " + transfer.amount() + " to " + transfer.to() + "\n```")
                    .collect(Collectors.joining());
            embed.addField("Transfers for " + payer, body, false);
        });
    }

    private Color colorFor(long balance) {
        if (balance > 0) {
            return PROFIT_COLOR;
        }
        if (balance < 0) {
            return WASTE_COLOR;
        }
        return NEUTRAL_COLOR;
    }

    private String format(long value) {
        return NUMBER_FORMAT.format(value);
    }
}
