package com.healthcareapp.hms.security;

import com.healthcareapp.hms.entity.Patient; // To create AppUserDetails
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
// Mockito needed only if mocking Authentication/UserDetails, otherwise remove @Mock below
// import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils; // Import ReflectionTestUtils

import javax.crypto.SecretKey; // Import SecretKey
import java.util.Date;
import java.util.List;
import java.util.Base64; // For creating test secret

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within; // *** ADDED Import ***
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    // Test configuration values
    private final String testSecret = Base64.getEncoder().encodeToString("MySuperSecretKeyForTestingPurposesLalalalaHohohohoABCDEF1234567890".getBytes());
    private final String differentSecret = Base64.getEncoder().encodeToString("DifferentSecretKeyForTestingSignatureFailureABCDEF1234567890XYZ".getBytes());
    private final int testExpirationMs = 3600000; // 1 hour

    private Authentication mockAuthentication;
    private AppUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        // Manually set the @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", testExpirationMs);

        // Create a real AppUserDetails instance for testing claims
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setEmail("test@patient.com");
        patient.setPassword("hashed");
        patient.setName("Test Patient");
        testUserDetails = new AppUserDetails(patient); // Has ROLE_PATIENT

        // Mock the Authentication object
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testUserDetails);
    }

    // Helper to get the SecretKey using the (now package-private) key() method
    private SecretKey getTestKey() {
        return jwtTokenProvider.key();
    }
    // Helper to get a SecretKey from a specific base64 string
    private SecretKey keyFromString(String base64Secret) {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(base64Secret));
    }


    @Test
    @DisplayName("Generate Token Success")
    void testGenerateToken() {
        // Act
        String token = jwtTokenProvider.generateToken(mockAuthentication);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();

        // Decode and verify claims using the same key
        Claims claims = Jwts.parser()
                .verifyWith(getTestKey()) // Use helper to call package-private method
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(testUserDetails.getUsername());
        assertThat(claims.get("userId", Long.class)).isEqualTo(testUserDetails.getUserId());
        assertThat(claims.get("userType", String.class)).isEqualTo(testUserDetails.getUserType());
        assertThat(claims.get("name", String.class)).isEqualTo(testUserDetails.getName());
        assertThat(claims.get("roles", String.class)).isEqualTo("ROLE_PATIENT");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull().isAfter(new Date());

        // *** UPDATED Expiration Check ***
        long expectedExpiryTime = new Date().getTime() + testExpirationMs;
        assertThat(claims.getExpiration().getTime()).isCloseTo(expectedExpiryTime, within(10000L)); // Use within() and Long offset
    }

    @Test
    @DisplayName("Validate Token - Valid Token")
    void testValidateToken_Valid() {
        // Arrange
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        // Act & Assert
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Validate Token - Expired Token")
    void testValidateToken_Expired() {
        // Arrange
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000); // Expire 1 second ago
        String expiredToken = jwtTokenProvider.generateToken(mockAuthentication);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", testExpirationMs); // Reset for other tests

        // Act & Assert
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }



    @Test
    @DisplayName("Get Username From JWT Success")
    void testGetUsernameFromJWT_Success() {
        // Arrange
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        // Act
        String username = jwtTokenProvider.getUsernameFromJWT(token);
        // Assert
        assertThat(username).isEqualTo(testUserDetails.getUsername());
    }

    @Test
    @DisplayName("Get Username From JWT Failure (Expired)")
    void testGetUsernameFromJWT_Expired() {
        // Arrange
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000);
        String expiredToken = jwtTokenProvider.generateToken(mockAuthentication);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", testExpirationMs);

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> {
            jwtTokenProvider.getUsernameFromJWT(expiredToken);
        });
    }

    @Test
    @DisplayName("Get Claims From JWT Success")
    void testGetClaimsFromJWT_Success() {
        // Arrange
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        // Act
        Claims claims = jwtTokenProvider.getClaimsFromJWT(token);
        // Assert
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUserDetails.getUsername());
        assertThat(claims.get("userId", Long.class)).isEqualTo(testUserDetails.getUserId());
        // ... assert other claims ...
    }

}