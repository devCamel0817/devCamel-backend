package dev.camel.backendlab.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    @Test
    void parseAllowedOriginsTrimsAndKeepsExplicitOrigins() {
        String[] origins = CorsConfig.parseAllowedOrigins(" http://localhost:5173 , https://devcamel.dev ");

        assertThat(origins).containsExactly("http://localhost:5173", "https://devcamel.dev");
    }

    @Test
    void parseAllowedOriginsRejectsWildcard() {
        assertThatThrownBy(() -> CorsConfig.parseAllowedOrigins("*"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Wildcard CORS origin");
    }

    @Test
    void parseAllowedOriginsRejectsBlankInput() {
        assertThatThrownBy(() -> CorsConfig.parseAllowedOrigins("   ,   "))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must contain at least one explicit origin");
    }
}

