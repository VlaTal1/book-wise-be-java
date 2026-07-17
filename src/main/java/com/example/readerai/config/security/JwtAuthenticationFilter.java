package com.example.readerai.config.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public JwtAuthenticationFilter(String supabaseUrl) {
        this.jwtProcessor = createJwtProcessor(supabaseUrl);
    }

    private ConfigurableJWTProcessor<SecurityContext> createJwtProcessor(String supabaseUrl) {
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";
            log.info("Loading JWKS from: {}", jwksUrl);

            JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));
            log.info("JWKS loaded successfully, {} key(s) found", jwkSet.getKeys().size());

            JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, keySource);

            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);

            return processor;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWKS from Supabase: " + supabaseUrl, e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Убираем "Bearer "

        try {
            JWTClaimsSet claims = jwtProcessor.process(token, null);

            String userId = claims.getSubject(); // Получаем user ID
            log.debug("JWT validated successfully for user: {}", userId);

            User user = new User(userId, "", Collections.emptyList());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
