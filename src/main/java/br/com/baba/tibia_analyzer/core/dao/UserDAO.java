package br.com.baba.tibia_analyzer.core.dao;

import br.com.baba.tibia_analyzer.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserDAO extends JpaRepository<User, UUID> {

    Optional<User> findByDiscordId(String discordId);
}
