package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.discord.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.discord.exception.ConverterException;
import br.com.baba.tibia_analyzer.discord.util.PartyAnalyzerConverter;
import br.com.baba.tibia_analyzer.discord.util.PartyHuntSplitter;
import org.springframework.stereotype.Service;

@Service
public class PartyHuntService {

    public String processSession(String input) throws ConverterException {
        PartyHuntAnalyzerDTO analyzerDTO = PartyAnalyzerConverter.getAnalyzer(input);
        String adjustmentsMessage = PartyHuntSplitter.split(analyzerDTO);
        return adjustmentsMessage;
    }
}
