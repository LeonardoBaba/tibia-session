package br.com.baba.tibia_analyzer.core.dao;

import br.com.baba.tibia_analyzer.api.dto.SessionFilter;
import br.com.baba.tibia_analyzer.core.model.PartyMember;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class PartySessionSpecifications {

    private PartySessionSpecifications() {}

    public static Specification<PartySession> fromFilter(SessionFilter filter) {
        return Specification.allOf(
                nameLike(filter.name()),
                playerLike(filter.player()),
                startTimeAfterOrEqual(filter.startDate()),
                startTimeBeforeOrEqual(filter.endDate()),
                ownerEquals(filter.ownerDiscordId())
        );
    }

    private static Specification<PartySession> nameLike(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    private static Specification<PartySession> playerLike(String player) {
        if (player == null || player.isBlank()) {
            return null;
        }
        String pattern = "%" + player.toLowerCase() + "%";
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                query.distinct(true);
            }
            Join<PartySession, PartyMember> member = root.join("partyMembers");
            return cb.like(cb.lower(member.get("name")), pattern);
        };
    }

    private static Specification<PartySession> startTimeAfterOrEqual(java.time.LocalDateTime startDate) {
        if (startDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), startDate);
    }

    private static Specification<PartySession> startTimeBeforeOrEqual(java.time.LocalDateTime endDate) {
        if (endDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startTime"), endDate);
    }

    private static Specification<PartySession> ownerEquals(String ownerDiscordId) {
        if (ownerDiscordId == null || ownerDiscordId.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("ownerDiscordId"), ownerDiscordId);
    }
}
