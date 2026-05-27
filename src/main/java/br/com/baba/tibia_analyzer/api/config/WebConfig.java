package br.com.baba.tibia_analyzer.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebConfig {

    /**
     * Origens autorizadas a chamar a API. Para liberar mais ambientes (preview,
     * staging etc.) basta acrescentar nesta lista.
     */
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:4200",
            "https://huntanalyzer.lbaba.com.br"
    );

    /**
     * Bean usado tanto pela Spring MVC quanto pela Spring Security (via
     * `http.cors(...)`), garantindo uma fonte única de configuração de CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Necessário pro browser anexar o cookie da sessão em requests
        // cross-subdomain (front em huntanalyzer.lbaba.com.br chamando
        // api.huntanalyzer.lbaba.com.br).
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
