package br.com.baba.tibia_analyzer.core.dao;

import br.com.baba.tibia_analyzer.core.model.PartySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface PartySessionDAO extends JpaRepository<PartySession, UUID>,
        JpaSpecificationExecutor<PartySession> {

    @Override
    @EntityGraph(attributePaths = "partyMembers")
    Page<PartySession> findAll(Specification<PartySession> spec, Pageable pageable);
}
