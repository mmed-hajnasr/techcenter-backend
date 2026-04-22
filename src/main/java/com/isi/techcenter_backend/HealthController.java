package com.isi.techcenter_backend;

import java.util.Map;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final Tracer tracer;

    public HealthController(Tracer tracer) {
        this.tracer = tracer;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Span span = tracer.nextSpan().name("health.check").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("endpoint", "/health");
            return Map.of("status", "UP");
        } finally {
            span.end();
        }
    }
}
