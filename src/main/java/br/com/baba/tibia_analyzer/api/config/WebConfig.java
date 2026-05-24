package br.com.baba.tibia_analyzer.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Origens autorizadas a chamar a API. Para liberar mais ambientes (preview,
     * staging etc.) basta acrescentar nesta lista.
     */
    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:4200",
            "https://huntanalyzer.lbaba.com.br"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
