package br.com.baba.tibia_analyzer.core.service;

import br.com.baba.tibia_analyzer.core.dao.UserDAO;
import br.com.baba.tibia_analyzer.core.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO dao;

    @InjectMocks
    private UserService service;

    @Test
    void shouldCreateUserWhenDiscordIdIsUnknown() {
        when(dao.findByDiscordId("123")).thenReturn(Optional.empty());
        when(dao.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.upsertFromDiscord("123", "Hikeppo", "https://cdn/avatar.png");

        Assertions.assertEquals("123", result.getDiscordId());
        Assertions.assertEquals("Hikeppo", result.getUsername());
        Assertions.assertEquals("https://cdn/avatar.png", result.getAvatarUrl());
        Assertions.assertNotNull(result.getCreatedAt());
        Assertions.assertNotNull(result.getUpdatedAt());
        verify(dao).save(any(User.class));
    }

    @Test
    void shouldUpdateUsernameAndAvatarWhenChanged() {
        Instant originalCreated = Instant.parse("2025-01-01T00:00:00Z");
        Instant originalUpdated = Instant.parse("2025-01-01T00:00:00Z");
        User existing = new User();
        existing.setDiscordId("123");
        existing.setUsername("OldName");
        existing.setAvatarUrl("https://cdn/old.png");
        existing.setCreatedAt(originalCreated);
        existing.setUpdatedAt(originalUpdated);
        when(dao.findByDiscordId("123")).thenReturn(Optional.of(existing));
        when(dao.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.upsertFromDiscord("123", "NewName", "https://cdn/new.png");

        Assertions.assertEquals("NewName", result.getUsername());
        Assertions.assertEquals("https://cdn/new.png", result.getAvatarUrl());
        Assertions.assertEquals(originalCreated, result.getCreatedAt());
        Assertions.assertNotEquals(originalUpdated, result.getUpdatedAt());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(dao).save(captor.capture());
        Assertions.assertEquals("NewName", captor.getValue().getUsername());
    }

    @Test
    void shouldNotSaveWhenNothingChanged() {
        User existing = new User();
        existing.setDiscordId("123");
        existing.setUsername("SameName");
        existing.setAvatarUrl("https://cdn/same.png");
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());
        when(dao.findByDiscordId("123")).thenReturn(Optional.of(existing));

        User result = service.upsertFromDiscord("123", "SameName", "https://cdn/same.png");

        Assertions.assertSame(existing, result);
        verify(dao, never()).save(any(User.class));
    }
}
