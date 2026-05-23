package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PartyAnalyzerConverter {

    private static final DateTimeFormatter ANALYZER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

    public PartyHuntAnalyzerDTO getAnalyzer(String input) {
        String normalizedInput = input.replaceAll("\\s*\\n\\s*", " ").trim();

        LocalDateTime startTime = parseDateTime(extractValue(normalizedInput, "From (.*?)(?:,)? to"));
        LocalDateTime endTime = parseDateTime(extractValue(normalizedInput, "to (.*?) Session:"));
        String sessionDuration = extractValue(normalizedInput, "Session: (.*?) Loot Type:");
        long loot = parseLongValue(extractValue(normalizedInput, "Loot: ([\\d,]+)"));
        long supplies = parseLongValue(extractValue(normalizedInput, "Supplies: ([\\d,]+)"));
        long balance = parseLongValue(extractValue(normalizedInput, "Balance: (-?[\\d,]+)"));

        String playerRegex = "([A-Za-z ()]+)\\s*Loot:\\s*([\\d,]+)\\s*Supplies:\\s*([\\d,]+)\\s*Balance:\\s*(-?[\\d,]+)\\s*Damage:\\s*([\\d,]+)\\s*Healing:\\s*([\\d,]+)";
        Pattern playerPattern = Pattern.compile(playerRegex);
        Matcher playerMatcher = playerPattern.matcher(normalizedInput);

        List<PlayerDTO> players = new ArrayList<>();

        while (playerMatcher.find()) {
            String name = playerMatcher.group(1).replace("(Leader)", "").trim();
            long pLoot = parseLongValue(playerMatcher.group(2));
            long pSupplies = parseLongValue(playerMatcher.group(3));
            long pBalance = parseLongValue(playerMatcher.group(4));
            long pDamage = parseLongValue(playerMatcher.group(5));
            long pHealing = parseLongValue(playerMatcher.group(6));

            players.add(new PlayerDTO(name, pLoot, pSupplies, pBalance, pDamage, pHealing));
        }

        return new PartyHuntAnalyzerDTO(startTime, endTime, sessionDuration, loot, supplies, balance, players);
    }

    private long parseLongValue(String value) {
        return Long.parseLong(value.replace(",", ""));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, ANALYZER_DATE_FORMAT);
    }

    private String extractValue(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
