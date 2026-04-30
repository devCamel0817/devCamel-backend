package dev.camel.backendlab.common.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PingControllerTest {

    private final PingController controller = new PingController();

    @Test
    void pingReturnsStableHealthPayload() {
        Map<String, Object> response = controller.ping();

        assertThat(response)
            .containsEntry("status", "OK")
            .containsEntry("service", "backend-lab")
            .containsEntry("message", "pong");
        assertThat(response.get("timestamp")).isInstanceOf(Long.class);
    }
}

