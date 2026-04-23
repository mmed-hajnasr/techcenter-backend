package com.isi.techcenter_backend.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    public static final String JWT_RESULT_ATTRIBUTE = "jwt.filter.result";
    public static final String JWT_ROLE_ATTRIBUTE = "jwt.filter.role";
    public static final String JWT_USER_ID_ATTRIBUTE = "jwt.filter.userId";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean tokenValid = false;
        String resolvedRole = "none";
        String resolvedUserId = null;
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                var authenticatedUser = jwtService.extractAuthenticatedUser(token);

                if (authenticatedUser.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var principal = authenticatedUser.get();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal.userId().toString(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    tokenValid = true;
                    resolvedRole = principal.role().name();
                    resolvedUserId = principal.userId().toString();
                } else {
                    resolvedRole = "unknown";
                }
            }
        } catch (RuntimeException exception) {
            log.error("auth.jwt.middleware result=error errorType={} path={} method={}",
                    exception.getClass().getSimpleName(),
                    request.getRequestURI(),
                    request.getMethod());
            throw exception;
        } finally {
            String validationResult = tokenValid ? "valid" : "invalid";
            request.setAttribute(JWT_RESULT_ATTRIBUTE, validationResult);
            request.setAttribute(JWT_ROLE_ATTRIBUTE, resolvedRole);
            if (resolvedUserId != null) {
                request.setAttribute(JWT_USER_ID_ATTRIBUTE, resolvedUserId);
            }
            log.info(
                    "auth.jwt.validation result={} role={} path={} method={}",
                    validationResult,
                    resolvedRole,
                    request.getRequestURI(),
                    request.getMethod());
        }

        filterChain.doFilter(request, response);
    }
}
