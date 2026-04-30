package com.isi.techcenter_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiServerProperties(String serverUrl) {
}
