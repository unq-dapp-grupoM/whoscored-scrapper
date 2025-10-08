package com.dapp.whoscored_scrapper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para APIs stateless
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll() // Permite acceso a todos los endpoints bajo /api/
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Permite acceso a Swagger UI y la definición de la API
                .anyRequest().authenticated() // Requiere autenticación para cualquier otra petición
            )
            .httpBasic(withDefaults()); // Puedes cambiar esto por otra forma de autenticación si lo necesitas

        return http.build();
    }
}