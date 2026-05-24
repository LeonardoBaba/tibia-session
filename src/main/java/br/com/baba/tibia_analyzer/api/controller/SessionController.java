package br.com.baba.tibia_analyzer.api.controller;

import br.com.baba.tibia_analyzer.api.dto.CreateSessionRequest;
import br.com.baba.tibia_analyzer.api.dto.SessionDetailDTO;
import br.com.baba.tibia_analyzer.api.mapper.SessionMapper;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private PartyHuntService partyHuntService;

    @PostMapping
    public ResponseEntity<SessionDetailDTO> create(@RequestBody CreateSessionRequest request) {
        PartySession saved = partyHuntService.createSession(
                request.input(),
                request.name(),
                request.comment(),
                request.ownerDiscordId()
        );
        SessionDetailDTO body = SessionMapper.toDetail(saved);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailDTO> getById(@PathVariable UUID id) {
        return partyHuntService.findById(id)
                .map(SessionMapper::toDetail)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
