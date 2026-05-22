package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.core.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerBalanceDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerStatDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PartyHuntSplitter {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+):(\\d+)");

    public SessionResultDTO split(PartyHuntAnalyzerDTO analyzerDTO) {
        List<PlayerDTO> players = analyzerDTO.players();
        int memberCount = players.size();

        long totalLoot = players.stream().mapToLong(PlayerDTO::loot).sum();
        long totalSupplies = players.stream().mapToLong(PlayerDTO::supplies).sum();
        long balance = totalLoot - totalSupplies;
        long individualBalance = balance / memberCount;

        long lootPerHour = calculateLootPerHour(totalLoot, analyzerDTO.sessionDuration());
        List<PlayerStatDTO> damage = buildRanking(players, PlayerDTO::damage);
        List<PlayerStatDTO> healing = buildRanking(players, PlayerDTO::healing);
        List<AdjustmentDTO> transfers = calculateAdjustments(players, individualBalance);

        return new SessionResultDTO(memberCount, balance, individualBalance, lootPerHour,
                analyzerDTO.sessionDuration(), damage, healing, transfers);
    }

    private long calculateLootPerHour(long totalLoot, String sessionDuration) {
        Matcher matcher = DURATION_PATTERN.matcher(sessionDuration == null ? "" : sessionDuration);
        if (!matcher.find()) {
            return 0;
        }
        long hours = Long.parseLong(matcher.group(1));
        long minutes = Long.parseLong(matcher.group(2));
        double totalHours = hours + minutes / 60.0;
        if (totalHours == 0) {
            return 0;
        }
        return Math.round(totalLoot / totalHours);
    }

    private List<PlayerStatDTO> buildRanking(List<PlayerDTO> players, ToLongFunction<PlayerDTO> extractor) {
        long total = players.stream().mapToLong(extractor).sum();
        return players.stream()
                .map(player -> {
                    long value = extractor.applyAsLong(player);
                    double percentage = total == 0 ? 0.0 : value * 100.0 / total;
                    return new PlayerStatDTO(player.name(), value, percentage);
                })
                .sorted(Comparator.comparingLong(PlayerStatDTO::value).reversed())
                .toList();
    }

    private List<AdjustmentDTO> calculateAdjustments(List<PlayerDTO> players, long individualBalance) {
        List<AdjustmentDTO> adjustments = new ArrayList<>();

        List<PlayerBalanceDTO> balances = new ArrayList<>();
        for (PlayerDTO player : players) {
            long difference = individualBalance - player.balance();
            balances.add(new PlayerBalanceDTO(player.name(), difference));
        }

        balances.sort((a, b) -> Long.compare(a.balance(), b.balance()));
        int i = 0, j = balances.size() - 1;

        while (i < j) {
            PlayerBalanceDTO debtor = balances.get(i);
            PlayerBalanceDTO creditor = balances.get(j);

            long transfer = Math.min(-debtor.balance(), creditor.balance());
            adjustments.add(new AdjustmentDTO(debtor.name(), creditor.name(), transfer));

            long newDebtorBalance = debtor.balance() + transfer;
            long newCreditorBalance = creditor.balance() - transfer;

            balances.set(i, new PlayerBalanceDTO(debtor.name(), newDebtorBalance));
            balances.set(j, new PlayerBalanceDTO(creditor.name(), newCreditorBalance));

            if (newDebtorBalance == 0) i++;
            if (newCreditorBalance == 0) j--;
        }

        return adjustments;
    }
}
