package com.isi.techcenter_backend;

import java.util.List;

import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

    @Bean
    OtlpGrpcSpanExporter otlpGrpcSpanExporter(
            @Value("${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}") String endpoint) {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
    }

    @Bean(destroyMethod = "close")
    SdkTracerProvider sdkTracerProvider(
            OtlpGrpcSpanExporter spanExporter,
            @Value("${spring.application.name:techcenter-backend}") String serviceName) {
        Resource resource = Resource.getDefault().toBuilder()
                .putAll(Attributes.builder().put("service.name", serviceName).build())
                .build();

        return SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();
    }

    @Bean
    OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }

    @Bean
    Tracer tracer(OpenTelemetry openTelemetry) {
        io.opentelemetry.api.trace.Tracer otelTracer = openTelemetry.getTracer("health-tracer");
        OtelCurrentTraceContext currentTraceContext = new OtelCurrentTraceContext();
        BaggageManager baggageManager = new OtelBaggageManager(currentTraceContext, List.of(), List.of());
        return new OtelTracer(otelTracer, currentTraceContext, event -> {
        }, baggageManager);
    }
}
