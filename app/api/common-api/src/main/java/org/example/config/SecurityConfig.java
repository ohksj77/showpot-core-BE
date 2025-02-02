package org.example.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.exception.ErrorResponse;
import org.example.filter.ExceptionHandlerFilter;
import org.example.filter.JWTFilter;
import org.example.security.error.TokenError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTFilter jwtFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin((formLogin) -> formLogin
                .loginPage("/admin/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/admin/home")
            )
            .logout((logout) -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/home")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .defaultAuthenticationEntryPointFor(
                    (request, response, authException) -> {
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write(
                            ErrorResponse.businessErrorResponseBuilder()
                                .error(TokenError.WRONG_HEADER)
                                .errorId(UUID.randomUUID().toString())
                                .build()
                                .toString()
                        );
                    },
                    antMatcher("/admin/login")
                )
            )
            .authorizeHttpRequests(registry -> registry
                .requestMatchers(getMatcherForUserAndAdmin())
                .hasAnyRole("USER", "ADMIN")
                .requestMatchers(getMatcherForAnyone())
                .permitAll()
                .anyRequest()
                .hasAnyRole("ADMIN")
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(exceptionHandlerFilter, JWTFilter.class)
            .build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOriginPatterns(Collections.singletonList("*"));
            return config;
        };
    }

    private RequestMatcher getMatcherForAnyone() {
        return RequestMatchers.anyOf(
            antMatcher(HttpMethod.GET, "/actuator/prometheus"),
            antMatcher("/health"),
            antMatcher("/swagger-ui/**"),
            antMatcher("/v3/api-docs/**"),
            antMatcher("/admin/**"),
            antMatcher("/css/**"),
            antMatcher("/js/**"),
            antMatcher(HttpMethod.POST, "/api/v1/users/login"),
            antMatcher(HttpMethod.POST, "/api/v1/users/reissue"),
            antMatcher(HttpMethod.POST, "/admin/login"),
            antMatcher(HttpMethod.POST, "/admin/signup"),
            antMatcher(HttpMethod.GET, "/admin/home"),
            antMatcher(HttpMethod.GET, "/api/v1/artists"),
            antMatcher(HttpMethod.GET, "/api/v1/genres"),
            antMatcher(HttpMethod.GET, "/api/v1/shows"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/{showId}"),
            antMatcher(HttpMethod.GET, "/api/v1/artists/search/**"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/search/**"),
            antMatcher(HttpMethod.GET, "/api/v1/artists/filter"),
            antMatcher(HttpMethod.GET, "/api/v1/artists/filter-total-count"),
            antMatcher(HttpMethod.GET, "/api/v1/artists/unsubscriptions")
        );
    }

    private RequestMatcher getMatcherForUserAndAdmin() {
        return RequestMatchers.anyOf(
            antMatcher(HttpMethod.POST, "/api/v1/users/logout"),
            antMatcher(HttpMethod.POST, "/api/v1/users/withdrawal"),
            antMatcher(HttpMethod.GET, "/api/v1/users/profile"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/alerts/count"),
            antMatcher(HttpMethod.GET, "/api/v1/artists/subscriptions/count"),
            antMatcher(HttpMethod.GET, "/api/v1/genres/subscriptions/count"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/interests"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/interests/count"),
            antMatcher(HttpMethod.POST, "/api/v1/shows/{showId}/interests"),
            antMatcher(HttpMethod.POST, "/api/v1/shows/{showId}/uninterested"),
            antMatcher(HttpMethod.POST, "/api/v1/shows/{showId}/alert"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/alerts"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/{showId}/alert/reservations"),
            antMatcher(HttpMethod.GET, "/api/v1/shows/terminated/ticketing/count"),
            antMatcher(HttpMethod.POST, "/api/v1/genres/subscribe"),
            antMatcher(HttpMethod.POST, "/api/v1/genres/unsubscribe"),
            antMatcher(HttpMethod.GET, "/api/v1/genres/subscriptions"),
            antMatcher(HttpMethod.GET, "/api/v1/genres/unsubscriptions"),
            antMatcher(HttpMethod.POST, "/api/v1/artists/subscribe"),
            antMatcher(HttpMethod.POST, "/api/v1/artists/unsubscribe"),
            antMatcher(HttpMethod.GET, "/api/v1/artists/subscriptions"),
            antMatcher(HttpMethod.GET, "/api/v1/users/notifications"),
            antMatcher(HttpMethod.GET, "/api/v1/users/notifications/exist")
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
