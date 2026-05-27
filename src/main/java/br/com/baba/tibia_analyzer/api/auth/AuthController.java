package br.com.baba.tibia_analyzer.api.auth;

import br.com.baba.tibia_analyzer.api.dto.UserDTO;
import br.com.baba.tibia_analyzer.core.model.User;
import br.com.baba.tibia_analyzer.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Devolve as informações do usuário autenticado. 401 se não houver sessão.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@org.springframework.security.core.annotation.AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String discordId = principal.getAttribute("id");
        if (discordId == null) {
            return ResponseEntity.status(401).build();
        }
        return userService.findByDiscordId(discordId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    /**
     * Encerra a sessão atual. Não usa o SecurityContextLogoutHandler com
     * dependências adicionais — simplesmente invalida a HttpSession.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private UserDTO toDto(User user) {
        return new UserDTO(user.getDiscordId(), user.getUsername(), user.getAvatarUrl());
    }
}
