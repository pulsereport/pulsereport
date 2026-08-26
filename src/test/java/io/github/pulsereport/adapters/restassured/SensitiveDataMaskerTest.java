package io.github.pulsereport.adapters.restassured;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    public void maskerMasksSensitiveHeaders() {
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
    public void maskerPreservesNonSensitiveHeaders() {
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
    public void maskerHandlesNullHeaders() {
        List<String> sensitiveHeaders = Arrays.asList("Authorization");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(null, sensitiveHeaders);
        
        assertNotNull(maskedHeaders);
        assertFalse(maskedHeaders.iterator().hasNext());
    }

    @Test
    public void maskerHandlesEmptyHeaders() {
        Headers originalHeaders = new Headers();
        List<String> sensitiveHeaders = Arrays.asList("Authorization");
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
        
        assertNotNull(maskedHeaders);
        assertFalse(maskedHeaders.iterator().hasNext());
    }

    @Test
    public void maskerHandlesNullSensitiveList() {
        Headers originalHeaders = new Headers(
            new Header("Authorization", "Bearer secret"),
            new Header("Content-Type", "application/json")
        );
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, null);
        
        assertEquals("Bearer secret", maskedHeaders.getValue("Authorization"));
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
    }

    @Test
    public void maskerHandlesEmptySensitiveList() {
        Headers originalHeaders = new Headers(
            new Header("Authorization", "Bearer secret"),
            new Header("Content-Type", "application/json")
        );
        
        Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, Collections.emptyList());
        
        assertEquals("Bearer secret", maskedHeaders.getValue("Authorization"));
        assertEquals("application/json", maskedHeaders.getValue("Content-Type"));
    }

    @Test
    public void maskerIsCaseInsensitive() {
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
    public void maskerWithCookieAndSetCookie() {
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

    // ------------------------------------------------------------------
    // maskBody tests
    // ------------------------------------------------------------------

    private static final String SAMPLE_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJzdWIiOiIxMjM0NTY3ODkwIn0"
            + ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJVadQssw5c";

    @Test
    public void maskBody_flatJson() {
        String body = "{\"password\":\"hunter2\",\"user\":\"bob\"}";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains("hunter2"));
        assertTrue(masked.contains(SensitiveDataMasker.REDACTED_VALUE));
        assertTrue(masked.contains("bob"));
    }

    @Test
    public void maskBody_nestedJsonAndArrays() {
        String body = "{\"users\":[{\"token\":\"abc\"}],\"ok\":1}";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("token"));

        assertFalse(masked.contains("abc"));
        assertTrue(masked.contains(SensitiveDataMasker.REDACTED_VALUE));
        assertTrue(masked.contains("\"ok\":1"));
    }

    @Test
    public void maskBody_caseInsensitiveKeys() {
        String body = "{\"Password\":\"x\",\"TOKEN\":\"y\",\"user\":\"bob\"}";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password", "token"));

        assertFalse(masked.contains("\"x\""));
        assertFalse(masked.contains("\"y\""));
        assertTrue(masked.contains("bob"));
    }

    @Test
    public void maskBody_matchedScalarsRedactedUnmatchedScalarsUntouched() {
        String body = "{\"password\":null,\"attempts\":3}";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertNotNull(masked);
        assertTrue(masked.contains("\"password\":\"" + SensitiveDataMasker.REDACTED_VALUE + "\""),
                "null value of a matched field should be replaced with the redacted string");
        assertTrue(masked.contains("\"attempts\":3"),
                "numeric value of a non-matched field should be preserved");
    }

    @Test
    public void maskBody_malformedJsonFallsBackToRegex() {
        String body = "{\"msg\": \"" + SAMPLE_JWT; // unterminated -> invalid JSON

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertNotNull(masked);
        assertFalse(masked.contains(SAMPLE_JWT), "embedded JWT should be masked by regex fallback");
        assertTrue(masked.contains(SensitiveDataMasker.REDACTED_VALUE));
    }

    @Test
    public void maskBody_emptyBody() {
        assertEquals("", SensitiveDataMasker.maskBody("", Arrays.asList("password")));
    }

    @Test
    public void maskBody_nullBody() {
        assertNull(SensitiveDataMasker.maskBody(null, Arrays.asList("password")));
    }

    @Test
    public void maskBody_jwtInJsonValue() {
        String body = "{\"message\":\"" + SAMPLE_JWT + "\"}";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains(SAMPLE_JWT),
                "JWT in a non-sensitive field should still be masked by the regex pass");
        assertTrue(masked.contains(SensitiveDataMasker.REDACTED_VALUE));
    }

    @Test
    public void maskBody_bearerTokenInPlainText() {
        String body = "Authorization: Bearer " + SAMPLE_JWT;

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains(SAMPLE_JWT));
        assertTrue(masked.contains("Bearer " + SensitiveDataMasker.REDACTED_VALUE));
    }

    @Test
    public void maskBody_formUrlencodedSecret() {
        String body = "user=bob&client_secret=xyz&x=1";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("client_secret"));

        assertFalse(masked.contains("xyz"));
        assertTrue(masked.contains("client_secret=" + SensitiveDataMasker.REDACTED_VALUE));
        assertTrue(masked.contains("user=bob"));
        assertTrue(masked.contains("x=1"));
    }

    @Test
    public void maskBody_dottedNonTokensNotMasked() {
        String body = "version 1.2.3 and foo.bar.baz stay intact";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertEquals(body, masked);
    }

    @Test
    public void maskBody_xmlBodyMasksJwtAndSensitiveElements() {
        String body = "<auth><password>secret</password><data>" + SAMPLE_JWT + "</data><user>bob</user></auth>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains(SAMPLE_JWT), "embedded JWT should be masked by the regex pass");
        assertFalse(masked.contains("secret"),
                "elements matching the field list should be redacted by default");
        assertTrue(masked.contains("<password>" + SensitiveDataMasker.REDACTED_VALUE + "</password>"));
        assertTrue(masked.contains("<auth>"));
        assertTrue(masked.contains("<user>bob</user>"));
    }

    @Test
    public void maskBody_xmlMasksSensitiveElement() {
        String body = "<login><password>secret</password></login>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains("<password>" + SensitiveDataMasker.REDACTED_VALUE + "</password>"));
        assertTrue(masked.contains("<login>"), "output should still be parseable XML");
    }

    @Test
    public void maskBody_xmlMasksNestedElements() {
        String body = "<a><b><c><password>deep-secret</password></c></b></a>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains("deep-secret"));
        assertTrue(masked.contains("<password>" + SensitiveDataMasker.REDACTED_VALUE + "</password>"));
        assertTrue(masked.contains("<a><b><c>"), "nesting structure should be preserved");
    }

    @Test
    public void maskBody_xmlCaseInsensitiveTagMatch() {
        String body = "<login><Password>secret</Password></login>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains("<Password>" + SensitiveDataMasker.REDACTED_VALUE + "</Password>"));
    }

    @Test
    public void maskBody_xmlNonSensitiveElementsUnchanged() {
        String body = "<r><name>bob</name><city>NY</city></r>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertTrue(masked.contains("<name>bob</name>"));
        assertTrue(masked.contains("<city>NY</city>"));
        assertFalse(masked.contains(SensitiveDataMasker.REDACTED_VALUE));
    }

    @Test
    public void maskBody_xmlAttributesNotMasked() {
        String body = "<user password=\"x\">text</user>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertTrue(masked.contains("password=\"x\""),
                "attribute values must NOT be redacted");
        assertTrue(masked.contains(">text</user>"),
                "element tag 'user' is not in the list, so its text stays unchanged");
    }

    @Test
    public void maskBody_xmlDisabledLeavesElementsVisible() {
        String body = "<login><password>secret</password><token>" + SAMPLE_JWT + "</token></login>";

        String masked = SensitiveDataMasker.maskBody(body,
                Arrays.asList("password"), Arrays.asList("password"), false, true);

        assertTrue(masked.contains("<password>secret</password>"),
                "with maskXmlEnabled=false, XML element text must NOT be redacted");
        assertFalse(masked.contains(SAMPLE_JWT),
                "JWT in the XML should still be token-masked when maskTokens=true");
    }

    @Test
    public void maskBody_xmlInvalidFallsBackToTokenMasking() {
        String body = "<login><password>" + SAMPLE_JWT; // malformed XML

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"));

        assertNotNull(masked);
        assertFalse(masked.contains(SAMPLE_JWT), "JWT should be redacted via regex fallback");
        assertTrue(masked.contains(SensitiveDataMasker.REDACTED_VALUE));
    }

    @Test
    public void maskBody_xmlXxeDoctypeNotExpanded() {
        String body = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE r [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n"
                + "<r>&xxe;</r>";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("xxe"));

        assertNotNull(masked);
        assertTrue(masked.contains("&xxe;"),
                "DOCTYPE payloads should be rejected by parser hardening; original body returned");
        assertFalse(masked.contains("root:"), "entity must not be expanded");
    }

    @Test
    public void maskBody_xmlUsesDedicatedXmlFieldsList() {
        String body = "<r><token>a</token><secret>b</secret></r>";

        String masked = SensitiveDataMasker.maskBody(body,
                Arrays.asList("token"), Arrays.asList("secret"), true, true);

        assertTrue(masked.contains("<token>a</token>"),
                "fields from the body list must not drive XML masking");
        assertTrue(masked.contains("<secret>" + SensitiveDataMasker.REDACTED_VALUE + "</secret>"),
                "fields from the XML list must be redacted");
        assertFalse(masked.contains(">b</secret>"));
    }

    /**
     * The sensitiveFields list passed to maskBody is authoritative: it fully
     * replaces the default field list. Only fields in the given list are
     * masked.
     */
    @Test
    public void maskBody_customFieldList() {
        String body = "{\"ssn\":\"123-45-6789\",\"password\":\"hunter2\"}";

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("ssn"));

        assertFalse(masked.contains("123-45-6789"));
        assertTrue(masked.contains("hunter2"),
                "default fields not in the custom list must NOT be masked");
    }

    @Test
    public void maskBody_maskTokensDisabledLeavesJwtIntact() {
        String body = "Authorization: Bearer " + SAMPLE_JWT;

        String masked = SensitiveDataMasker.maskBody(body, Arrays.asList("password"), false);

        assertEquals(body, masked, "with maskTokens=false the regex passes must be skipped");
    }

    @Test
    public void maskBody_nullFieldListStillMasksTokens() {
        String body = "{\"message\":\"" + SAMPLE_JWT + "\"}";

        String masked = SensitiveDataMasker.maskBody(body, null);

        assertFalse(masked.contains(SAMPLE_JWT));
    }

    @Test
    public void maskBody_masksNumericValueForSensitiveField() {
        String masked = SensitiveDataMasker.maskBody(
                "{\"pin\": 1234}", Arrays.asList("pin"));

        assertTrue(masked.contains("\"pin\":\"***REDACTED***\""),
                "numeric value of a sensitive field should be replaced with the redacted string");
    }

    @Test
    public void maskBody_masksBooleanValueForSensitiveField() {
        String masked = SensitiveDataMasker.maskBody(
                "{\"secret\": true}", Arrays.asList("secret"));

        assertTrue(masked.contains("\"secret\":\"***REDACTED***\""),
                "boolean value of a sensitive field should be replaced with the redacted string");
    }

    @Test
    public void maskBody_masksNullValueForSensitiveField() {
        String masked = SensitiveDataMasker.maskBody(
                "{\"token\": null}", Arrays.asList("token"));

        assertTrue(masked.contains("\"token\":\"***REDACTED***\""),
                "null value of a sensitive field should be replaced with the redacted string");
    }

    @Test
    public void maskBody_nonSensitiveScalarsUnchanged() {
        String masked = SensitiveDataMasker.maskBody(
                "{\"id\": 42, \"active\": true}", Arrays.asList("password"));

        assertTrue(masked.contains("\"id\":42"),
                "non-sensitive numeric values should be unchanged");
        assertTrue(masked.contains("\"active\":true"),
                "non-sensitive boolean values should be unchanged");
        assertFalse(masked.contains("***REDACTED***"));
    }
}
