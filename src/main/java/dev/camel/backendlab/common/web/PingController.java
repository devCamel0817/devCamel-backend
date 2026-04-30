package dev.camel.backendlab.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/ping")
    public LinkedHashMap<String, Object> ping() {
        LinkedHashMap<String, Object> response = new LinkedHashMap<String,Object>();
        response.put("status", "OK");
        response.put("service", "backend-lab");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "pong");

        return response;
    }
}
