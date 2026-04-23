package com.isi.techcenter_backend;

import java.util.Map;

import com.isi.techcenter_backend.tracing.EndpointTraceSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final EndpointTraceSupport endpointTraceSupport;

    public HealthController(EndpointTraceSupport endpointTraceSupport) {
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return endpointTraceSupport.inSpan(
                "health.check",
                "/health",
                "health-check",
                () -> Map.of("status", "UP"));
    }
}
