package br.com.baba.tibia_analyzer.core.util;

import br.com.baba.tibia_analyzer.discord.dto.AdjustmentDTO;
import br.com.baba.tibia_analyzer.discord.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.discord.dto.PlayerBalanceDTO;
import br.com.baba.tibia_analyzer.discord.dto.PlayerDTO;

import java.util.ArrayList;
import java.util.List;

public class PartyHuntSplitter {
    public static void split(PartyHuntAnalyzerDTO analyzerDTO) {
        long totalLoot = 0;
        long totalSupplies = 0;

        for (PlayerDTO player : analyzerDTO.getPlayers()) {
            totalLoot += player.getLoot();
            totalSupplies += player.getSupplies();
        }

        StringBuilder result = new StringBuilder();
        result.append("Total Loot: ").append(totalLoot).append("\n");
        result.append("Total Supplies: ").append(totalSupplies).append("\n");

        long netProfit = totalLoot - totalSupplies;
        result.append("Total: ").append(netProfit).append("\n\n");

        int totalPlayers = analyzerDTO.getPlayers().size();
        long equalShare = netProfit / totalPlayers;

        List<AdjustmentDTO> adjustments = calculateAdjustments(analyzerDTO.getPlayers(), equalShare);
        String player = "";
        for (AdjustmentDTO adjustment : adjustments) {
            if (!player.equals(adjustment.getFrom())) {
                result.append(adjustment.getFrom()).append(System.lineSeparator());
                player = adjustment.getFrom();
            }
            result.append("transfer ").append(adjustment.getAmount()).append(" to ").append(adjustment.getTo()).append(System.lineSeparator());
        }

        analyzerDTO.setProcessedMessage(result.toString());
    }

    private static List<AdjustmentDTO> calculateAdjustments(List<PlayerDTO> players, long equalShare) {
        List<AdjustmentDTO> adjustments = new ArrayList<>();

        List<PlayerBalanceDTO> balances = new ArrayList<>();
        for (PlayerDTO player : players) {
            long difference = equalShare - player.getBalance();
            balances.add(new PlayerBalanceDTO(player.getName(), difference));
        }

        balances.sort((a, b) -> Long.compare(a.getBalance(), b.getBalance()));
        int i = 0, j = balances.size() - 1;

        while (i < j) {
            PlayerBalanceDTO debtor = balances.get(i);
            PlayerBalanceDTO creditor = balances.get(j);

            long transfer = Math.min(-debtor.getBalance(), creditor.getBalance());
            adjustments.add(new AdjustmentDTO(debtor.getName(), creditor.getName(), transfer));

            debtor.setBalance(debtor.getBalance() + transfer);
            creditor.setBalance(creditor.getBalance() - transfer);

            if (debtor.getBalance() == 0) i++;
            if (creditor.getBalance() == 0) j--;
        }

        return adjustments;
    }
}
