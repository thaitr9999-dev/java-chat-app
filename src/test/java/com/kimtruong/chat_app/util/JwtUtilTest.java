package com.kimtruong.chat_app.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil class.
 * Tests token generation, validation, and username extraction.
 * Does NOT require Spring context — uses ReflectionTestUtils to inject @Value fields.
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-min-32-chars-long!!";
    private static final long TEST_EXPIRATION_MS = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject @Value fields using ReflectionTestUtils (no Spring context needed)
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", TEST_EXPIRATION_MS);
    }

    // ========== generateToken Tests ==========

    @Test
    void generateToken_withValidUsername_returnsNonNullToken() {
        // Arrange
        String username = "testuser";

        // Act
        String token = jwtUtil.generateToken(username);

        // Assert
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isBlank(), "Generated token should not be empty");
        assertTrue(token.contains("."), "JWT token should have 3 parts separated by dots");
    }

    @Test
    void generateToken_withDifferentUsernames_generatesDifferentTokens() {
        // Arrange
        String username1 = "user1";
        String username2 = "user2";

        // Act
        String token1 = jwtUtil.generateToken(username1);
        String token2 = jwtUtil.generateToken(username2);

        // Assert
        assertNotEquals(token1, token2, "Different usernames should generate different tokens");
    }

    // ========== extractUsername Tests ==========

    @Test
    void extractUsername_withValidToken_returnsCorrectUsername() {
        // Arrange
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // Act
        String extractedUsername = jwtUtil.extractUsername(token);

        // Assert
        assertEquals(username, extractedUsername, "Extracted username should match original");
    }

    @Test
    void extractUsername_withDifferentTokens_extractsDifferentUsernames() {
        // Arrange
        String username1 = "alice";
        String username2 = "bob";
        String token1 = jwtUtil.generateToken(username1);
        String token2 = jwtUtil.generateToken(username2);

        // Act
        String extracted1 = jwtUtil.extractUsername(token1);
        String extracted2 = jwtUtil.extractUsername(token2);

        // Assert
        assertEquals(username1, extracted1);
        assertEquals(username2, extracted2);
        assertNotEquals(extracted1, extracted2);
    }

    // ========== isValid Tests ==========

    @Test
    void isValid_withValidToken_returnsTrue() {
        // Arrange
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // Act
        boolean isValid = jwtUtil.isValid(token);

        // Assert
        assertTrue(isValid, "Valid token should return true");
    }

    @Test
    void isValid_withMalformedToken_returnsFalse() {
        // Arrange
        String malformedToken = "not.a.valid.token";

        // Act
        boolean isValid = jwtUtil.isValid(malformedToken);

        // Assert
        assertFalse(isValid, "Malformed token should return false");
    }

    @Test
    void isValid_withEmptyToken_returnsFalse() {
        // Arrange
        String emptyToken = "";

        // Act
        boolean isValid = jwtUtil.isValid(emptyToken);

        // Assert
        assertFalse(isValid, "Empty token should return false");
    }

    @Test
    void isValid_withNullToken_returnsFalse() {
        // Arrange
        String nullToken = null;

        // Act & Assert
        assertFalse(jwtUtil.isValid(nullToken), "Null token should return false");
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        // Arrange
        String username = "testuser";
        String validToken = jwtUtil.generateToken(username);
        // Tamper with the token by changing one character
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "X";

        // Act
        boolean isValid = jwtUtil.isValid(tamperedToken);

        // Assert
        assertFalse(isValid, "Tampered token should fail validation");
    }

    @Test
    void isValid_withTokenFromDifferentSecret_returnsFalse() {
        // Arrange
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // Change secret and try to validate
        ReflectionTestUtils.setField(jwtUtil, "secret", "different-secret-key-min-32-chars!!!");

        // Act
        boolean isValid = jwtUtil.isValid(token);

        // Assert
        assertFalse(isValid, "Token with different secret should fail validation");
    }

    // ========== Token Content Tests ==========

    @Test
    void generateToken_tokenContainsCorrectClaims() {
        // Arrange
        String username = "testuser";

        // Act
        String token = jwtUtil.generateToken(username);
        String extracted = jwtUtil.extractUsername(token);

        // Assert
        assertEquals(username, extracted, "Token should contain correct username claim");
    }

    @Test
    void generateToken_createsValidTokenFormat() {
        // Arrange
        String username = "testuser";

        // Act
        String token = jwtUtil.generateToken(username);
        String[] parts = token.split("\\.");

        // Assert
        assertEquals(3, parts.length, "JWT should have 3 parts (header.payload.signature)");
        for (String part : parts) {
            assertFalse(part.isEmpty(), "Each JWT part should not be empty");
        }
    }

    // ========== Integration-like Tests ==========

    @Test
    void roundTrip_generateAndExtract_maintainsConsistency() {
        // Arrange
        String[] usernames = {"alice", "bob", "charlie", "user123"};

        for (String username : usernames) {
            // Act
            String token = jwtUtil.generateToken(username);
            String extracted = jwtUtil.extractUsername(token);
            boolean isValid = jwtUtil.isValid(token);

            // Assert
            assertEquals(username, extracted, "Username should be preserved in token");
            assertTrue(isValid, "Token should be valid");
        }
    }

    @Test
    void multipleTokens_eachTokenIsValidIndependently() {
        // Arrange
        String[] usernames = {"user1", "user2", "user3"};
        String[] tokens = new String[usernames.length];

        // Act - Generate tokens
        for (int i = 0; i < usernames.length; i++) {
            tokens[i] = jwtUtil.generateToken(usernames[i]);
        }

        // Assert - Each token is valid and contains correct username
        for (int i = 0; i < tokens.length; i++) {
            assertTrue(jwtUtil.isValid(tokens[i]));
            assertEquals(usernames[i], jwtUtil.extractUsername(tokens[i]));
        }
    }
}
