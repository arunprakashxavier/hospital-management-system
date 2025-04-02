package com.healthcareapp.hms.security;

import com.healthcareapp.hms.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter; // Important base class

import java.io.IOException;

@Component // Register as a Spring bean
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Ensure extending OncePerRequestFilter

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService; // To load UserDetails from token info

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Extract JWT from the request header
            String jwt = getJwtFromRequest(request);

            // 2. Validate the token
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 3. Get username (email) from token
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // 4. Load UserDetails associated with the token
                // (It's crucial that CustomUserDetailsService can load by email)
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // 5. Create Authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()); // Use loaded UserDetails

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Set the Authentication object in the SecurityContext
                // Spring Security now knows the user is authenticated for this request
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            // Don't throw exception here, just log. Let the request proceed.
            // If authentication is not set, subsequent security checks will deny access if required.
        }

        // 7. Continue the filter chain
        filterChain.doFilter(request, response);
    }

    // Helper method to extract token from "Authorization: Bearer <token>" header
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}