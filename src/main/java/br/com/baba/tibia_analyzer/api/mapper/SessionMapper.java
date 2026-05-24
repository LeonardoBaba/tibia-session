package br.com.baba.tibia_analyzer.api.mapper;

import br.com.baba.tibia_analyzer.api.dto.SessionDetailDTO;
import br.com.baba.tibia_analyzer.api.dto.SessionMemberDTO;
import br.com.baba.tibia_analyzer.api.dto.SessionSummaryDTO;
import br.com.baba.tibia_analyzer.api.dto.SessionTransferDTO;
import br.com.baba.tibia_analyzer.core.model.PartyMember;
import br.com.baba.tibia_analyzer.core.model.PartySession;
import br.com.baba.tibia_analyzer.core.model.PartyTransfer;

import java.util.List;

public final class SessionMapper {

    private SessionMapper() {}

    public static SessionSummaryDTO toSummary(PartySession session) {
        return new SessionSummaryDTO(
                session.getId(),
                session.getName(),
                session.getComment(),
                session.getOwnerDiscordId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getSessionDuration(),
                session.getLoot(),
                session.getSupplies(),
                session.getBalance(),
                session.getProcessDate() == null ? null : session.getProcessDate().toInstant()
        );
    }

    public static SessionDetailDTO toDetail(PartySession session) {
        List<SessionMemberDTO> members = session.getPartyMembers().stream()
                .map(SessionMapper::toMember)
                .toList();

        List<SessionTransferDTO> transfers = session.getPartyTransfers().stream()
                .map(SessionMapper::toTransfer)
                .toList();

        return new SessionDetailDTO(
                session.getId(),
                session.getName(),
                session.getComment(),
                session.getOwnerDiscordId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getSessionDuration(),
                session.getLoot(),
                session.getSupplies(),
                session.getBalance(),
                session.getProcessDate() == null ? null : session.getProcessDate().toInstant(),
                members,
                transfers
        );
    }

    private static SessionMemberDTO toMember(PartyMember member) {
        return new SessionMemberDTO(
                member.getId(),
                member.getName(),
                member.getLoot(),
                member.getSupplies(),
                member.getBalance(),
                member.getDamage(),
                member.getHealing()
        );
    }

    private static SessionTransferDTO toTransfer(PartyTransfer transfer) {
        return new SessionTransferDTO(
                transfer.getId(),
                transfer.getFromPlayer(),
                transfer.getToPlayer(),
                transfer.getAmount()
        );
    }
}
