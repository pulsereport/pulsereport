package io.github.pulsereport.adapters.restassured;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import io.restassured.http.Header;
import io.restassured.http.Headers;

/**
 * Tests for SensitiveDataMasker utility class.
 * Verifies sensitive header masking functionality.
 * 
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class SensitiveDataMaskerTest {

    @Test
    public void testMaskerMasksSensitiveHeaders() {
        Headers originalHeaders = new Headers(
            new Header("Content-Type", "application/json"),
            new Header("Authorization", "Bearer secret-token-123"),
            new Header("X-API-Key", "my-secret-api-key"),
            new Header("User-Agent", "RestAssured")
        );
        
        List<String> sensitiveHeaders = Arrays.asList("Authorization", "X-API-Key");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
        
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
        assertEquals("***REDACTED***", maskedHeaders.getValue("Authorization"));
        assertEquals("***REDACTED***", maskedHeaders.getValue("X-API-Key"));
        assertEquals("RestAssured", maskedHeaders.getValue("User-Agent"));
    }

    @Test
    public void testMaskerPreservesNonSensitiveHeaders() {
        Headers originalHeaders = new Headers(
            new Header("Content-Type", "application/json"),
            new Header("Accept", "application/json"),
            new Header("User-Agent", "RestAssured")
        );
        
        List<String> sensitiveHeaders = Arrays.asList("Authorization", "X-API-Key");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
        
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
        assertEquals("application/json", maskedHeaders.getValue("Accept"));
        assertEquals("RestAssured", maskedHeaders.getValue("User-Agent"));
    }

    @Test
    public void testMaskerHandlesNullHeaders() {
        List<String> sensitiveHeaders = Arrays.asList("Authorization");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(null, sensitiveHeaders);
        
        assertNotNull(maskedHeaders);
        assertFalse(maskedHeaders.iterator().hasNext());
    }

    @Test
    public void testMaskerHandlesEmptyHeaders() {
        Headers originalHeaders = new Headers();
        List<String> sensitiveHeaders = Arrays.asList("Authorization");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
        
        assertNotNull(maskedHeaders);
        assertFalse(maskedHeaders.iterator().hasNext());
    }

    @Test
    public void testMaskerHandlesNullSensitiveList() {
        Headers originalHeaders = new Headers(
            new Header("Authorization", "Bearer secret"),
            new Header("Content-Type", "application/json")
        );
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, null);
        
        assertEquals("Bearer secret", maskedHeaders.getValue("Authorization"));
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
    }

    @Test
    public void testMaskerHandlesEmptySensitiveList() {
        Headers originalHeaders = new Headers(
            new Header("Authorization", "Bearer secret"),
            new Header("Content-Type", "application/json")
        );
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, Collections.emptyList());
        
        assertEquals("Bearer secret", maskedHeaders.getValue("Authorization"));
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
    }

    @Test
    public void testMaskerIsCaseInsensitive() {
        Headers originalHeaders = new Headers(
            new Header("authorization", "Bearer secret"),
            new Header("X-Api-Key", "secret-key"),
            new Header("COOKIE", "session=abc123")
        );
        
        List<String> sensitiveHeaders = Arrays.asList("Authorization", "X-API-Key", "Cookie");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
        
        assertEquals("***REDACTED***", maskedHeaders.getValue("authorization"));
        assertEquals("***REDACTED***", maskedHeaders.getValue("X-Api-Key"));
        assertEquals("***REDACTED***", maskedHeaders.getValue("COOKIE"));
    }

    @Test
    public void testMaskerWithCookieAndSetCookie() {
        Headers originalHeaders = new Headers(
            new Header("Cookie", "session=xyz; user=admin"),
            new Header("Set-Cookie", "token=abc123; Path=/; HttpOnly"),
            new Header("Content-Type", "application/json")
        );
        
        List<String> sensitiveHeaders = Arrays.asList("Cookie", "Set-Cookie");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
        
        assertEquals("***REDACTED***", maskedHeaders.getValue("Cookie"));
        assertEquals("***REDACTED***", maskedHeaders.getValue("Set-Cookie"));
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
    }
}
