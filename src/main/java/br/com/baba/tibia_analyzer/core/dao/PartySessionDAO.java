package br.com.baba.tibia_analyzer.core.dao;

import br.com.baba.tibia_analyzer.core.model.PartySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PartySessionDAO extends JpaRepository<PartySession, UUID>,
        JpaSpecificationExecutor<PartySession> {

}
