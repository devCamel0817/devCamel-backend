package dev.camel.backendlab.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:}")
    private String rawAllowedOrigins;

    private String[] allowedOrigins;

    @PostConstruct
    void validateAllowedOrigins() {
        this.allowedOrigins = parseAllowedOrigins(rawAllowedOrigins);
    }

    static String[] parseAllowedOrigins(String rawAllowedOrigins) {
        String[] origins = Arrays.stream(rawAllowedOrigins == null ? new String[0] : rawAllowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);

        if (origins.length == 0) {
            throw new IllegalStateException("app.cors.allowed-origins must contain at least one explicit origin");
        }

        boolean hasWildcard = Arrays.stream(origins)
            .anyMatch(origin -> "*".equals(origin));
        if (hasWildcard) {
            throw new IllegalStateException("Wildcard CORS origin '*' is not allowed. Use explicit origins only.");
        }

        return origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "OPTIONS")
            // 와일드카드 대신 실제 사용 헤더만 명시 (자격증명 사용 시 "*" 사용 불가)
            .allowedHeaders("Content-Type", "Accept", "Origin", "X-Requested-With")
            .exposedHeaders("Content-Type")
            .allowCredentials(false)
            .maxAge(3600);
    }
}
