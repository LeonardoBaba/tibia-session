package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.core.dao.UserDAO;
import br.com.baba.tibia_analyzer.core.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserDAO dao;

    /**
     * Garante que existe um row em `users` pro Discord ID informado. Se já
     * existe, atualiza `username`/`avatarUrl` caso tenham mudado (Discord
     * permite trocar a qualquer momento). Retorna a entidade persistida.
     */
    public User upsertFromDiscord(String discordId, String username, String avatarUrl) {
        Optional<User> existing = dao.findByDiscordId(discordId);
        Instant now = Instant.now();

        if (existing.isPresent()) {
            User user = existing.get();
            boolean changed = false;
            if (username != null && !username.equals(user.getUsername())) {
                user.setUsername(username);
                changed = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                changed = true;
            }
            if (changed) {
                user.setUpdatedAt(now);
                return dao.save(user);
            }
            return user;
        }

        User created = new User();
        created.setDiscordId(discordId);
        created.setUsername(username);
        created.setAvatarUrl(avatarUrl);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        return dao.save(created);
    }

    public Optional<User> findByDiscordId(String discordId) {
        return dao.findByDiscordId(discordId);
    }
}
