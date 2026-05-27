package br.com.baba.tibia_analyzer.api.auth;

import br.com.baba.tibia_analyzer.api.config.SecurityConfig;
import br.com.baba.tibia_analyzer.api.config.WebConfig;
import br.com.baba.tibia_analyzer.core.model.User;
import br.com.baba.tibia_analyzer.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({WebConfig.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "ANALYZER_SERVER_PORT=0",
        "ANALYZER_DATABASE_CONNECTION=jdbc:postgresql://localhost/test",
        "ANALYZER_DATABASE_USER=test",
        "ANALYZER_DATABASE_PASSWORD=test",
        "ANALYZER_DISCORD_TOKEN=dummy",
        "ANALYZER_DISCORD_CLIENT_ID=test-client-id",
        "ANALYZER_DISCORD_CLIENT_SECRET=test-client-secret",
        "ANALYZER_FRONTEND_URL=http://localhost:4200"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private DiscordOAuth2UserService discordOAuth2UserService;

    @Test
    void shouldReturnUserDtoWhenAuthenticated() throws Exception {
        User user = new User();
        user.setDiscordId("123");
        user.setUsername("Hikeppo");
        user.setAvatarUrl("https://cdn/avatar.png");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        when(userService.findByDiscordId("123")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me")
                        .with(oauth2Login().attributes(attrs -> attrs.put("id", "123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discordId").value("123"))
                .andExpect(jsonPath("$.username").value("Hikeppo"))
                .andExpect(jsonPath("$.avatarUrl").value("https://cdn/avatar.png"));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenAuthenticatedButUserNotInDb() throws Exception {
        when(userService.findByDiscordId("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me")
                        .with(oauth2Login().attributes(attrs -> attrs.put("id", "999"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn204OnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
