
package com.mealtrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealtrack.config.CorsOrigins;
import com.mealtrack.exception.ApiError;
import com.mealtrack.security.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtFilter jwtFilter,
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.jwtFilter = jwtFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    // Tratamento padronizado de erros do Spring Security
    private void escreverErro(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String mensagem
    ) throws IOException {

        ApiError erro = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                Map.of()
        );

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), erro);
    }

    // Configuração principal de segurança
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Tratamento dos erros 401 e 403
                .exceptionHandling(ex -> ex

                        // 401 - Não autenticado
                        .authenticationEntryPoint((request, response, exception) ->
                                escreverErro(
                                        request,
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Autenticação necessária. Informe um token JWT válido."
                                )
                        )

                        // 403 - Sem permissão
                        .accessDeniedHandler((request, response, exception) ->
                                escreverErro(
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "Você não possui permissão para acessar este recurso."
                                )
                        )
                )

                // Regras de autorização
                .authorizeHttpRequests(auth -> auth

                        // Swagger e documentação
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Acesso público
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/usuarios/login").permitAll()
                        .requestMatchers(
                                "/monitoramento/**",
                                "/simulacao/**"
                        ).permitAll()

                        // Alimentos
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/alimentos/**"
                        ).hasAnyRole("ADMIN", "SUPERVISOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/alimentos/**"
                        ).hasAnyRole("ADMIN", "SUPERVISOR")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/alimentos/**"
                        ).hasAnyRole("ADMIN", "SUPERVISOR")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/alimentos/**"
                        ).authenticated()

                        // Produções
                        .requestMatchers("/producoes/**")
                        .hasAnyRole("ADMIN", "SUPERVISOR", "FUNCIONARIO")

                        // Relatórios
                        .requestMatchers(
                                HttpMethod.GET,
                                "/relatorios/**"
                        ).authenticated()

                        // Dashboard
                        .requestMatchers(
                                HttpMethod.GET,
                                "/dashboard"
                        ).authenticated()

                        // Usuários e restaurantes
                        .requestMatchers(
                                "/usuarios/**",
                                "/restaurantes/**"
                        ).hasRole("ADMIN")

                        // Demais endpoints
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // Autenticação
    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    // Configuração CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(CorsOrigins.PERMITIDAS);

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
