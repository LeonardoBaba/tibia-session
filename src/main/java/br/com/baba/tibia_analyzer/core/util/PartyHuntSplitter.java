package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.discord.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.discord.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.discord.dto.PlayerBalanceDTO;
import br.com.baba.tibia_analyzer.discord.dto.PlayerDTO;

import java.util.ArrayList;
import java.util.List;

public class PartyHuntSplitter {
    public static PartyHuntAnalyzerDTO split(PartyHuntAnalyzerDTO analyzerDTO) {
        long totalLoot = 0;
        long totalSupplies = 0;

        for (PlayerDTO player : analyzerDTO.players()) {
            totalLoot += player.loot();
            totalSupplies += player.supplies();
        }

        StringBuilder result = new StringBuilder();
        result.append("Total Loot: ").append(totalLoot).append("\n");
        result.append("Total Supplies: ").append(totalSupplies).append("\n");

        long netProfit = totalLoot - totalSupplies;
        result.append("Total: ").append(netProfit).append("\n\n");

        int totalPlayers = analyzerDTO.players().size();
        long equalShare = netProfit / totalPlayers;

        List<AdjustmentDTO> adjustments = calculateAdjustments(analyzerDTO.players(), equalShare);
        String player = "";
        for (AdjustmentDTO adjustment : adjustments) {
            if (!player.equals(adjustment.from())) {
                result.append(adjustment.from()).append(System.lineSeparator());
                player = adjustment.from();
            }
            result.append("transfer ").append(adjustment.amount()).append(" to ").append(adjustment.to()).append(System.lineSeparator());
        }

        return new PartyHuntAnalyzerDTO(analyzerDTO, result.toString());
    }

    private static List<AdjustmentDTO> calculateAdjustments(List<PlayerDTO> players, long equalShare) {
        List<AdjustmentDTO> adjustments = new ArrayList<>();

        List<PlayerBalanceDTO> balances = new ArrayList<>();
        for (PlayerDTO player : players) {
            long difference = equalShare - player.balance();
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
