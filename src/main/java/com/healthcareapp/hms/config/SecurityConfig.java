package com.healthcareapp.hms.config;

import com.healthcareapp.hms.service.CustomUserDetailsService;
import com.healthcareapp.hms.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order; // Import Order
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    // Inject dependencies needed by both chains
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // --- Filter Chain 1: APIs, Admin, Doctors (Higher Precedence) ---
    @Bean
    @Order(1)
    public SecurityFilterChain apiAndDoctorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/admin/**", "/doctor/**", "/perform_doctor_login")
                .csrf(csrf -> csrf.disable())
                // Default session policy (IF_REQUIRED) will apply now.
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/doctors/specialization/**", "GET"),
                                new AntPathRequestMatcher("/api/doctors/*/available-slots", "GET")
                        ).permitAll()
                        .requestMatchers("/doctor/login").permitAll() // Allow access to login page URL
                        .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/appointments/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/doctor/login")
                        .loginProcessingUrl("/perform_doctor_login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(new SimpleUrlAuthenticationSuccessHandler("/doctor/dashboard"))
                        .failureUrl("/doctor/login?error=true")
                )
                .logout(logout -> logout.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // --- Filter Chain 2: Patients & General Web (Lower Precedence) ---
    @Bean
    @Order(2)
    public SecurityFilterChain patientFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Public web pages and resources
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", // Static resources
                                "/", "/home", "/patient/register",
                                "/patient/login", "/doctor/login",
                                "/admin/login", // *** ADDED /admin/login HERE ***
                                "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**" // Swagger UI
                        ).permitAll()
                        // Secure patient web pages
                        .requestMatchers("/patient/**").hasRole("PATIENT")
                        // Secure everything else by default for this chain
                        .anyRequest().authenticated()
                )
                // --- Patient Login Configuration (Scoped to this chain) ---
                .formLogin(form -> form
                        .loginPage("/patient/login")
                        .loginProcessingUrl("/perform_patient_login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(new SimpleUrlAuthenticationSuccessHandler("/patient/dashboard"))
                        .failureUrl("/patient/login?error=true")
                )
                // --- General Logout Configuration ---
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}