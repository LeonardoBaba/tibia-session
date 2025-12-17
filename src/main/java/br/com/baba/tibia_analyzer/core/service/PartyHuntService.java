package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.core.dao.PartySessionDAO;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.discord.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.exception.ConverterException;
import br.com.baba.tibia_analyzer.core.util.PartyAnalyzerConverter;
import br.com.baba.tibia_analyzer.core.util.PartyHuntSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PartyHuntService {

    @Autowired
    private PartySessionDAO dao;

    public String processSession(String input) throws ConverterException {
        PartyHuntAnalyzerDTO analyzerDTO = PartyHuntSplitter.split(PartyAnalyzerConverter.getAnalyzer(input));
        dao.save(new PartySession(analyzerDTO, input));
        return Optional.ofNullable(analyzerDTO.processedMessage()).orElse("Error processing session");
    }
}
