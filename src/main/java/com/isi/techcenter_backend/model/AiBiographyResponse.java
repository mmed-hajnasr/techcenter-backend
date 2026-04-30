package com.isi.techcenter_backend.model;

import java.util.List;

public record AiBiographyResponse(
        String name,
        String biography,
        List<String> researchAreas) {
}
