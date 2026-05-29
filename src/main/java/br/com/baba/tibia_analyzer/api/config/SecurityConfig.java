package br.com.baba.tibia_analyzer.api.config;

import br.com.baba.tibia_analyzer.api.auth.DiscordOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    @Autowired
    private DiscordOAuth2UserService discordOAuth2UserService;

    @Value("${analyzer.frontend.url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF desabilitado: cookies SameSite=Lax + CORS restrito por origem
                // dão a proteção necessária pra esse SPA.
                .csrf(AbstractHttpConfigurer::disable)
                // Mantém o CORS configurado em WebConfig.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos do OAuth (login redirect, callback).
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // GETs em sessões são públicos (qualquer um vê pela URL).
                        .requestMatchers(HttpMethod.GET, "/api/sessions", "/api/sessions/**").permitAll()
                        // `/me` decide internamente entre 200 e 401, e logout
                        // pode ser chamado a qualquer hora.
                        .requestMatchers("/api/auth/me", "/api/auth/logout").permitAll()
                        // Preview não persiste nada — público. Precisa vir
                        // antes do matcher de POST /api/sessions.
                        .requestMatchers(HttpMethod.POST, "/api/sessions/preview").permitAll()
                        // Mutações precisam de sessão autenticada.
                        .requestMatchers(HttpMethod.POST, "/api/sessions").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/sessions/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/sessions/**").authenticated()
                        .anyRequest().permitAll())
                // Sem auth → 401 (e não redireciona pro provider; quem dispara
                // o login é o front via window.location).
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(discordOAuth2UserService))
                        .defaultSuccessUrl(frontendUrl + "/", true)
                        .failureUrl(frontendUrl + "/?login=failed"));

        return http.build();
    }
}
