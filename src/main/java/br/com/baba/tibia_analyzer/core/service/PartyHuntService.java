package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.api.dto.SessionFilter;
import br.com.baba.tibia_analyzer.core.dao.PartySessionDAO;
import br.com.baba.tibia_analyzer.core.dao.PartySessionSpecifications;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.util.PartyAnalyzerConverter;
import br.com.baba.tibia_analyzer.core.util.PartyHuntSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PartyHuntService {

    @Autowired
    private PartySessionDAO dao;

    @Autowired
    private PartyAnalyzerConverter partyHuntAnalyzerConverter;

    @Autowired
    private PartyHuntSplitter partyHuntSplitter;

    public SessionResultDTO processSession(String input) {
        return processSession(input, null, null, null);
    }

    public SessionResultDTO processSession(String input,
                                           String name,
                                           String comment,
                                           String ownerDiscordId) {
        return processAndPersist(input, name, comment, ownerDiscordId).splitterResult();
    }

    public PartySession createSession(String input,
                                      String name,
                                      String comment,
                                      String ownerDiscordId) {
        return processAndPersist(input, name, comment, ownerDiscordId).saved();
    }

    public ProcessedSession processAndPersist(String input,
                                              String name,
                                              String comment,
                                              String ownerDiscordId) {
        PartyHuntAnalyzerDTO analyzerDTO = partyHuntAnalyzerConverter.getAnalyzer(input);
        SessionResultDTO result = partyHuntSplitter.split(analyzerDTO);
        PartySession saved = dao.save(
                new PartySession(analyzerDTO, result, input, name, comment, ownerDiscordId));
        return new ProcessedSession(saved, result);
    }

    public Optional<PartySession> findById(UUID id) {
        return dao.findById(id);
    }

    public PartySession previewSession(String input) {
        PartyHuntAnalyzerDTO analyzerDTO = partyHuntAnalyzerConverter.getAnalyzer(input);
        SessionResultDTO result = partyHuntSplitter.split(analyzerDTO);
        return new PartySession(analyzerDTO, result, input, null, null, null);
    }

    public Page<PartySession> list(SessionFilter filter, Pageable pageable) {
        return dao.findAll(PartySessionSpecifications.fromFilter(filter), pageable);
    }

    public Optional<PartySession> updateMetadata(UUID id, String name, String comment) {
        return dao.findById(id).map(session -> {
            if (name != null) {
                session.setName(name);
            }
            if (comment != null) {
                session.setComment(comment);
            }
            return dao.save(session);
        });
    }

    public boolean delete(UUID id) {
        if (!dao.existsById(id)) {
            return false;
        }
        dao.deleteById(id);
        return true;
    }

    public record ProcessedSession(PartySession saved, SessionResultDTO splitterResult) {}
}
