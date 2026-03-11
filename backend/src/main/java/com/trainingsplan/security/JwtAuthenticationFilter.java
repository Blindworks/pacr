package com.trainingsplan.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String method = request.getMethod();
        final String uri = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        log.debug("[JWT] {} {} — Authorization header: {}", method, uri,
                authHeader != null ? (authHeader.startsWith("Bearer ") ? "Bearer ***" : authHeader) : "MISSING");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT] {} {} — no Bearer token, passing through (will be rejected by security if protected)", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            username = jwtService.extractUsername(jwt);
            log.debug("[JWT] {} {} — extracted username='{}'", method, uri, username);
        } catch (Exception e) {
            log.warn("[JWT] {} {} — token extraction failed: {}", method, uri, e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[JWT] {} {} — authenticated as '{}'", method, uri, username);
                } else {
                    log.warn("[JWT] {} {} — token invalid or expired for user '{}'", method, uri, username);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Expired or invalid token");
                    return;
                }
            } catch (UsernameNotFoundException ex) {
                log.warn("[JWT] {} {} — unknown user '{}'", method, uri, username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown user");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
