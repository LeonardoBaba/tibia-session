package br.com.baba.tibia_analyzer.api.controller;

import br.com.baba.tibia_analyzer.api.dto.SessionFilter;
import br.com.baba.tibia_analyzer.api.exception.ApiExceptionHandler;
import br.com.baba.tibia_analyzer.core.dto.PartyHuntAnalyzerDTO;
import br.com.baba.tibia_analyzer.core.dto.PlayerDTO;
import br.com.baba.tibia_analyzer.core.dto.SessionResultDTO;
import br.com.baba.tibia_analyzer.core.exception.ConverterException;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import(ApiExceptionHandler.class)
@TestPropertySource(properties = {
        "ANALYZER_SERVER_PORT=0",
        "ANALYZER_DATABASE_CONNECTION=jdbc:postgresql://localhost/test",
        "ANALYZER_DATABASE_USER=test",
        "ANALYZER_DATABASE_PASSWORD=test",
        "ANALYZER_DISCORD_TOKEN=dummy"
})
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartyHuntService partyHuntService;

    @Test
    void shouldCreateSessionAndReturn201WithDetail() throws Exception {
        PartySession saved = buildSession();
        when(partyHuntService.createSession(eq("raw input"), eq("Cobra Bastion"), eq("note"), eq("owner-1")))
                .thenReturn(saved);

        String body = """
                {
                  "input": "raw input",
                  "name": "Cobra Bastion",
                  "comment": "note",
                  "ownerDiscordId": "owner-1"
                }
                """;

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/sessions/" + saved.getId())))
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Cobra Bastion"))
                .andExpect(jsonPath("$.comment").value("note"))
                .andExpect(jsonPath("$.ownerDiscordId").value("owner-1"))
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.members[0].name").value("Player1"));
    }

    @Test
    void shouldReturn400WhenParserFails() throws Exception {
        when(partyHuntService.createSession(any(), any(), any(), any()))
                .thenThrow(new NumberFormatException("For input string: \"\""));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": \"garbage\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400WhenConverterThrows() throws Exception {
        when(partyHuntService.createSession(any(), any(), any(), any()))
                .thenThrow(new ConverterException("Failed to parse"));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": \"garbage\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to parse"));
    }

    @Test
    void shouldReturn200WithDetailWhenSessionExists() throws Exception {
        PartySession saved = buildSession();
        when(partyHuntService.findById(saved.getId())).thenReturn(Optional.of(saved));

        mockMvc.perform(get("/api/sessions/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.transfers.length()").value(0));
    }

    @Test
    void shouldReturn404WhenSessionDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(partyHuntService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sessions/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnPagedListWithDefaultPagination() throws Exception {
        PartySession session = buildSession();
        Page<PartySession> page = new PageImpl<>(List.of(session), PageRequest.of(0, 20), 1);
        when(partyHuntService.list(any(SessionFilter.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(session.getId().toString()))
                .andExpect(jsonPath("$.content[0].name").value("Cobra Bastion"))
                .andExpect(jsonPath("$.content[0].balance").value(1000))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldPropagateFiltersAndPageableToService() throws Exception {
        Page<PartySession> empty = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        when(partyHuntService.list(any(SessionFilter.class), any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/api/sessions")
                        .param("name", "Cobra")
                        .param("player", "Hikeppo")
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-12-31T23:59:00")
                        .param("ownerDiscordId", "owner-1")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "balance,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<SessionFilter> filterCaptor = ArgumentCaptor.forClass(SessionFilter.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(partyHuntService)
                .list(filterCaptor.capture(), pageableCaptor.capture());

        SessionFilter filter = filterCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("Cobra", filter.name());
        org.junit.jupiter.api.Assertions.assertEquals("Hikeppo", filter.player());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), filter.startDate());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59), filter.endDate());
        org.junit.jupiter.api.Assertions.assertEquals("owner-1", filter.ownerDiscordId());

        Pageable pageable = pageableCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, pageable.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(5, pageable.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.ASC,
                pageable.getSort().getOrderFor("balance").getDirection());
    }

    @Test
    void shouldDefaultSortByStartTimeDescending() throws Exception {
        Page<PartySession> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(partyHuntService.list(any(SessionFilter.class), any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(partyHuntService)
                .list(any(SessionFilter.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order startTimeOrder = pageable.getSort().getOrderFor("startTime");
        org.junit.jupiter.api.Assertions.assertNotNull(startTimeOrder);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, startTimeOrder.getDirection());
    }

    private PartySession buildSession() throws Exception {
        PlayerDTO player = new PlayerDTO("Player1", 200, 100, 100, 5000, 1500);
        PartyHuntAnalyzerDTO analyzerDTO = new PartyHuntAnalyzerDTO(
                LocalDateTime.of(2025, 1, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 1, 11, 0, 0),
                "01:00h", 200, 100, 1000, List.of(player));
        SessionResultDTO result = new SessionResultDTO(
                1, 1000, 1000, 0, "01:00h", List.of(), List.of(), List.of());
        PartySession session = new PartySession(
                analyzerDTO, result, "raw input", "Cobra Bastion", "note", "owner-1");
        // forge an id since we're not going through JPA
        setField(session, "id", UUID.randomUUID());
        session.setProcessDate(new Date());
        return session;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
