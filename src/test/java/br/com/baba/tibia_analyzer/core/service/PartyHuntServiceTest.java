package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.core.dao.PartySessionDAO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.util.PartyAnalyzerConverter;
import br.com.baba.tibia_analyzer.core.util.PartyHuntSplitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyHuntServiceTest {

    @Mock
    private PartySessionDAO dao;

    @Mock
    private PartyAnalyzerConverter converter;

    @Mock
    private PartyHuntSplitter splitter;

    @InjectMocks
    private PartyHuntService service;

    @Test
    void shouldProcessSessionSuccessfully() {
        // Arrange
        String input = "raw input";
        PartyHuntAnalyzerDTO initialDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "dur", 0, 0, 0, Collections.emptyList(), null
        );
        
        PartyHuntAnalyzerDTO processedDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "dur", 0, 0, 0, Collections.emptyList(), "Success Message"
        );

        PartySession savedSession = new PartySession(processedDTO, input);
        savedSession.setProcessedMessage("Success Message");

        when(converter.getAnalyzer(input)).thenReturn(initialDTO);
        when(splitter.split(initialDTO)).thenReturn(processedDTO);
        when(dao.save(any(PartySession.class))).thenReturn(savedSession);

        // Act
        String result = service.processSession(input);

        // Assert
        Assertions.assertEquals("Success Message", result);
        
        verify(converter).getAnalyzer(input);
        verify(splitter).split(initialDTO);
        verify(dao).save(any(PartySession.class));
    }
}