package br.com.baba.tibia_analyzer.api.controller;

import br.com.baba.tibia_analyzer.api.dto.CreateSessionRequest;
import br.com.baba.tibia_analyzer.api.dto.PagedResponse;
import br.com.baba.tibia_analyzer.api.dto.SessionDetailDTO;
import br.com.baba.tibia_analyzer.api.dto.SessionFilter;
import br.com.baba.tibia_analyzer.api.dto.SessionSummaryDTO;
import br.com.baba.tibia_analyzer.api.dto.UpdateSessionRequest;
import br.com.baba.tibia_analyzer.api.mapper.SessionMapper;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.service.PartyHuntService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private PartyHuntService partyHuntService;

    @PostMapping
    public ResponseEntity<SessionDetailDTO> create(@RequestBody CreateSessionRequest request,
                                                   @AuthenticationPrincipal OAuth2User principal) {
        String ownerDiscordId = principal.getAttribute("id");

        PartySession saved = partyHuntService.createSession(
                request.input(),
                request.name(),
                request.comment(),
                ownerDiscordId
        );
        SessionDetailDTO body = SessionMapper.toDetail(saved);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(body);
    }

    @PostMapping("/preview")
    public SessionDetailDTO preview(@RequestBody CreateSessionRequest request) {
        PartySession transientSession = partyHuntService.previewSession(request.input());
        return SessionMapper.toDetail(transientSession);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailDTO> getById(@PathVariable UUID id) {
        return partyHuntService.findById(id)
                .map(SessionMapper::toDetail)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SessionDetailDTO> update(@PathVariable UUID id,
                                                   @RequestBody UpdateSessionRequest request,
                                                   @AuthenticationPrincipal OAuth2User principal) {
        return partyHuntService.findById(id)
                .map(existing -> {
                    if (!isOwner(existing, principal)) {
                        return ResponseEntity.status(403).<SessionDetailDTO>build();
                    }
                    return partyHuntService.updateMetadata(id, request.name(), request.comment())
                            .map(SessionMapper::toDetail)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal OAuth2User principal) {
        return partyHuntService.findById(id)
                .map(existing -> {
                    if (!isOwner(existing, principal)) {
                        return ResponseEntity.status(403).<Void>build();
                    }
                    return partyHuntService.delete(id)
                            ? ResponseEntity.noContent().<Void>build()
                            : ResponseEntity.notFound().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public PagedResponse<SessionSummaryDTO> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String player,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String ownerDiscordId,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SessionFilter filter = new SessionFilter(name, player, startDate, endDate, ownerDiscordId);
        Page<SessionSummaryDTO> page = partyHuntService.list(filter, pageable)
                .map(SessionMapper::toSummary);
        return PagedResponse.from(page);
    }

    private boolean isOwner(PartySession session, OAuth2User principal) {
        if (principal == null) return false;
        String discordId = principal.getAttribute("id");
        return discordId != null && discordId.equals(session.getOwnerDiscordId());
    }
}
