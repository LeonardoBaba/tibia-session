package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.discord.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.discord.dto.PlayerDTO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyAnalyzerConverter {

    public static PartyHuntAnalyzerDTO getAnalyzer(String input) {
        PartyHuntAnalyzerDTO session = new PartyHuntAnalyzerDTO();
 
        String normalizedInput = input.replaceAll("\\s*\\n\\s*", " ").trim();

        session.setStartTime(extractValue(normalizedInput, "From (.*?),"));
        session.setEndTime(extractValue(normalizedInput, "to (.*?) Session:"));
        session.setSessionDuration(extractValue(normalizedInput, "Session: (.*?) Loot Type:"));
        session.setLoot(parseLongValue(extractValue(normalizedInput, "Loot: ([\\d,]+)")));
        session.setSupplies(parseLongValue(extractValue(normalizedInput, "Supplies: ([\\d,]+)")));
        session.setBalance(parseLongValue(extractValue(normalizedInput, "Balance: (-?[\\d,]+)")));

        String playerRegex = "([A-Za-z ()]+)\\s*Loot:\\s*([\\d,]+)\\s*Supplies:\\s*([\\d,]+)\\s*Balance:\\s*(-?[\\d,]+)\\s*Damage:\\s*([\\d,]+)\\s*Healing:\\s*([\\d,]+)";
        Pattern playerPattern = Pattern.compile(playerRegex);
        Matcher playerMatcher = playerPattern.matcher(normalizedInput);

        while (playerMatcher.find()) {
            PlayerDTO player = new PlayerDTO();
            player.setName(playerMatcher.group(1).replace("(Leader)", "").trim());
            player.setLoot(parseLongValue(playerMatcher.group(2)));
            player.setSupplies(parseLongValue(playerMatcher.group(3)));
            player.setBalance(parseLongValue(playerMatcher.group(4)));
            player.setDamage(parseLongValue(playerMatcher.group(5)));
            player.setHealing(parseLongValue(playerMatcher.group(6)));

            session.addPlayer(player);
        }

        return session;
    }

    private static long parseLongValue(String value) {
        return Long.parseLong(value.replace(",", ""));
    }

    private static String extractValue(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
