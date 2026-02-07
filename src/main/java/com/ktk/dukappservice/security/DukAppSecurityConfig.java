package com.ktk.dukappservice.security;

import com.ktk.dukappservice.data.users.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.Map;

@EnableWebSecurity
@Configuration
public class DukAppSecurityConfig {

    private final UserService userService;

    public DukAppSecurityConfig(UserService userService) {
        this.userService = userService;
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        return new DukAppDetailsManager(userService);
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        String idForEncode = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        encoders.put("bcrypt", new BCryptPasswordEncoder());

        encoders.put("sha256", new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                return SecurityUtils.encryptSecret(raw.toString());
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                String rawString = raw.toString();
                // 1. Try matching raw (The new Path A way)
                String hashedRaw = SecurityUtils.encryptSecret(rawString);
                if (("{sha256}" + hashedRaw).equals(encoded)) return true;

                // 2. Try matching the raw input directly
                // (In case the DB still holds a double-hash from the old frontend logic)
                return rawString.equals(encoded);
            }
        });
        DelegatingPasswordEncoder delegatingEncoder = new DelegatingPasswordEncoder(idForEncode, encoders);

        delegatingEncoder.setDefaultPasswordEncoderForMatches(encoders.get("sha256"));

        return delegatingEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity, JwtAuthenticationFilter jwtFilter) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Enforces that all requests must be stateless by default
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().permitAll()
                )
                // Spring Boot 4 encourages granular filter ordering
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();

    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(
                        "https://dukapp.bcc-ktk.org",
                        "http://localhost:8999" // Match your flutter web port
                ).allowedHeaders("*").allowedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS");
            }
        };

    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }
}
