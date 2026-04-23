package com.isi.techcenter_backend.tracing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.isi.techcenter_backend.error.AuthException;
import com.isi.techcenter_backend.security.JwtAuthenticationFilter;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class EndpointTraceSupport {

    private static final Logger log = LoggerFactory.getLogger(EndpointTraceSupport.class);

    private final Tracer tracer;

    public EndpointTraceSupport(Tracer tracer) {
        this.tracer = tracer;
    }

    public <T> T inSpan(String spanName, String endpoint, String operation, Supplier<T> action, String... tagPairs) {
        Span span = tracer.nextSpan().name(spanName).start();
        long startNanos = System.nanoTime();
        Map<String, String> allTags = new LinkedHashMap<>();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            addTag(span, allTags, "endpoint", endpoint);
            addTag(span, allTags, "operation", operation);
            applyPairs(span, allTags, tagPairs);
            emitJwtFilterChildSpanIfAvailable();

            log.info("api.request operation={} endpoint={} tags={}", operation, endpoint, allTags);

            T response = action.get();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            addTag(span, allTags, "result", "success");
            log.info("api.response operation={} endpoint={} result=success durationMs={} tags={}",
                    operation,
                    endpoint,
                    durationMs,
                    allTags);
            return response;
        } catch (RuntimeException exception) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            boolean clientFailure = exception instanceof AuthException;
            addTag(span, allTags, "result", clientFailure ? "client_error" : "error");
            if (clientFailure) {
                log.info("api.response operation={} endpoint={} result=client_error durationMs={} errorType={} tags={}",
                        operation,
                        endpoint,
                        durationMs,
                        exception.getClass().getSimpleName(),
                        allTags);
            } else {
                span.error(exception);
                log.error("api.response operation={} endpoint={} result=error durationMs={} errorType={} tags={}",
                        operation,
                        endpoint,
                        durationMs,
                        exception.getClass().getSimpleName(),
                        allTags);
            }
            throw exception;
        } finally {
            span.end();
        }
    }

    private void applyPairs(Span span, Map<String, String> allTags, String... tagPairs) {
        if (tagPairs == null || tagPairs.length == 0) {
            return;
        }
        for (int index = 0; index + 1 < tagPairs.length; index += 2) {
            addTag(span, allTags, tagPairs[index], tagPairs[index + 1]);
        }
    }

    private void addTag(Span span, Map<String, String> allTags, String key, String value) {
        if (key == null || value == null) {
            return;
        }
        span.tag(key, value);
        allTags.put(key, value);
    }

    private void emitJwtFilterChildSpanIfAvailable() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        Object jwtResult = request.getAttribute(JwtAuthenticationFilter.JWT_RESULT_ATTRIBUTE);
        Object jwtRole = request.getAttribute(JwtAuthenticationFilter.JWT_ROLE_ATTRIBUTE);
        if (!(jwtResult instanceof String validationResult) || !(jwtRole instanceof String resolvedRole)) {
            return;
        }

        Span jwtSpan = tracer.nextSpan().name("auth.jwt.filter").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(jwtSpan)) {
            jwtSpan.tag("endpoint", request.getRequestURI());
            jwtSpan.tag("operation", "jwt-authentication");
            jwtSpan.tag("method", request.getMethod());
            jwtSpan.tag("jwt.result", validationResult);
            jwtSpan.tag("role", resolvedRole);

            Object userId = request.getAttribute(JwtAuthenticationFilter.JWT_USER_ID_ATTRIBUTE);
            if (userId instanceof String userIdValue) {
                jwtSpan.tag("userId", userIdValue);
            }

            jwtSpan.event("auth.jwt.validation result=" + validationResult + " role=" + resolvedRole);
        } finally {
            jwtSpan.end();
        }
    }
}