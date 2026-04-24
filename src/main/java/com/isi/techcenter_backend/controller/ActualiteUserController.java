package com.isi.techcenter_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isi.techcenter_backend.model.ActualiteUserResponse;
import com.isi.techcenter_backend.service.ActualiteUserService;
import com.isi.techcenter_backend.tracing.EndpointTraceSupport;

@RestController
@Validated
@RequestMapping("/user/actualites")
public class ActualiteUserController {

    private final ActualiteUserService actualiteUserService;
    private final EndpointTraceSupport endpointTraceSupport;

    public ActualiteUserController(
            ActualiteUserService actualiteUserService,
            EndpointTraceSupport endpointTraceSupport) {
        this.actualiteUserService = actualiteUserService;
        this.endpointTraceSupport = endpointTraceSupport;
    }

    @GetMapping
    public ResponseEntity<List<ActualiteUserResponse>> listActualites() {
        return endpointTraceSupport.inSpan(
                "user.actualites.list",
                "/user/actualites",
                "list-actualites",
                () -> ResponseEntity.ok(actualiteUserService.listActualites()));
    }
}
