package com.example.readerai.config.security;

import com.example.readerai.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ParticipantRepository participantRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        // Service-to-service від Python (guided-decoding сервіс) — захищено
                        // окремим API-ключем у контролері, не Supabase JWT.
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(supabaseUrl), UsernamePasswordAuthenticationFilter.class)
                // Після JWT: режим учня на телефоні вчителя (локальні учні).
                .addFilterAfter(new ActAsStudentFilter(participantRepository), JwtAuthenticationFilter.class);

        return http.build();
    }
}
