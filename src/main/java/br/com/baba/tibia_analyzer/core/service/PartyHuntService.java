package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.core.dao.PartySessionDAO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import br.com.baba.tibia_analyzer.core.exception.ConverterException;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.util.PartyAnalyzerConverter;
import br.com.baba.tibia_analyzer.core.util.PartyHuntSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartyHuntService {

    @Autowired
    private PartySessionDAO dao;

    @Autowired
    private PartyAnalyzerConverter partyHuntAnalyzerConverter;

    @Autowired
    private PartyHuntSplitter partyHuntSplitter;

    public SessionResultDTO processSession(String input) throws ConverterException {
        PartyHuntAnalyzerDTO analyzerDTO = partyHuntAnalyzerConverter.getAnalyzer(input);
        SessionResultDTO result = partyHuntSplitter.split(analyzerDTO);
        dao.save(new PartySession(analyzerDTO, result, input));
        return result;
    }
}
