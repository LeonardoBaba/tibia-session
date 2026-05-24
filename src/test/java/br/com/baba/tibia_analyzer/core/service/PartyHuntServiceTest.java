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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.baba.tibia_analyzer.api.dto.SessionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                null, null, "01:00h", 0, 0, 0, Collections.emptyList()
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

        ArgumentCaptor<PartySession> captor = ArgumentCaptor.forClass(PartySession.class);
        verify(dao).save(captor.capture());
        PartySession saved = captor.getValue();
        Assertions.assertNull(saved.getName());
        Assertions.assertNull(saved.getComment());
        Assertions.assertNull(saved.getOwnerDiscordId());
    }

    @Test
    void shouldPersistOptionalMetadataWhenProvided() {
        // Arrange
        String input = "raw input";
        PartyHuntAnalyzerDTO analyzerDTO = new PartyHuntAnalyzerDTO(
                null, null, "01:00h", 0, 0, 0, Collections.emptyList()
        );
        SessionResultDTO result = new SessionResultDTO(
                0, 0, 0, 0, "01:00h",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        when(converter.getAnalyzer(input)).thenReturn(analyzerDTO);
        when(splitter.split(analyzerDTO)).thenReturn(result);

        // Act
        service.processSession(input, "Cobra Bastion", "Boss died -1", "discord-123");

        // Assert
        ArgumentCaptor<PartySession> captor = ArgumentCaptor.forClass(PartySession.class);
        verify(dao).save(captor.capture());
        PartySession saved = captor.getValue();
        Assertions.assertEquals("Cobra Bastion", saved.getName());
        Assertions.assertEquals("Boss died -1", saved.getComment());
        Assertions.assertEquals("discord-123", saved.getOwnerDiscordId());
    }

    @Test
    void shouldReturnSavedSessionWhenCreatingSession() {
        // Arrange
        String input = "raw input";
        PartyHuntAnalyzerDTO analyzerDTO = new PartyHuntAnalyzerDTO(
                null, null, "01:00h", 0, 0, 0, Collections.emptyList()
        );
        SessionResultDTO result = new SessionResultDTO(
                0, 0, 0, 0, "01:00h",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
        PartySession persisted = new PartySession(analyzerDTO, result, input, "Hunt", "ok", "owner-1");

        when(converter.getAnalyzer(input)).thenReturn(analyzerDTO);
        when(splitter.split(analyzerDTO)).thenReturn(result);
        when(dao.save(any(PartySession.class))).thenReturn(persisted);

        // Act
        PartySession returned = service.createSession(input, "Hunt", "ok", "owner-1");

        // Assert
        Assertions.assertSame(persisted, returned);
    }

    @Test
    void shouldDelegateFindByIdToDao() {
        UUID id = UUID.randomUUID();
        PartySession session = new PartySession();
        when(dao.findById(id)).thenReturn(Optional.of(session));

        Optional<PartySession> result = service.findById(id);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(session, result.get());
        verify(dao).findById(id);
    }

    @Test
    void shouldDelegateListToDaoWithSpecificationAndPageable() {
        SessionFilter filter = new SessionFilter(
                "Cobra", "Hikeppo",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59),
                "owner-1");
        Pageable pageable = PageRequest.of(0, 10);
        PartySession session = new PartySession();
        Page<PartySession> expected = new PageImpl<>(List.of(session));

        when(dao.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<PartySession> result = service.list(filter, pageable);

        Assertions.assertSame(expected, result);
        verify(dao).findAll(any(Specification.class), eq(pageable));
    }
}
