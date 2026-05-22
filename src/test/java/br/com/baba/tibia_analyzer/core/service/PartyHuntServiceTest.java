package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.core.dao.PartySessionDAO;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
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
        PartyHuntAnalyzerDTO analyzerDTO = new PartyHuntAnalyzerDTO(
                "start", "end", "01:00h", 0, 0, 0, Collections.emptyList()
        );
        SessionResultDTO result = new SessionResultDTO(
                0, 0, 0, 0, "01:00h",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        when(converter.getAnalyzer(input)).thenReturn(analyzerDTO);
        when(splitter.split(analyzerDTO)).thenReturn(result);

        // Act
        SessionResultDTO returned = service.processSession(input);

        // Assert
        Assertions.assertSame(result, returned);

        verify(converter).getAnalyzer(input);
        verify(splitter).split(analyzerDTO);
        verify(dao).save(any(PartySession.class));
    }
}
