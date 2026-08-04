package io.github.pulsereport.adapters.restassured;

import java.util.ArrayList;
import java.util.List;

import io.restassured.http.Header;
import io.restassured.http.Headers;

/**
 * Utility class for masking sensitive header values in HTTP requests/responses.
 *
 * <p>
 * This class provides functionality to replace sensitive header values with a
 * redacted placeholder to prevent sensitive data (like authentication tokens,
 * API keys, cookies) from appearing in test reports.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Headers originalHeaders = new Headers(
 *     new Header("Authorization", "Bearer secret-token"),
 *     new Header("Content-Type", "application/json")
 * );
 *
 * List<String> sensitiveHeaders = Arrays.asList("Authorization", "X-API-Key");
 * Headers maskedHeaders = SensitiveDataMasker.maskHeaders(originalHeaders, sensitiveHeaders);
 * // Authorization header will have value "***REDACTED***"
 * }</pre>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class SensitiveDataMasker {

    /**
     * The string used to replace sensitive header values.
     */
    public static final String REDACTED_VALUE = "***REDACTED***";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SensitiveDataMasker() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Masks sensitive headers by replacing their values with a redacted
     * placeholder.
     *
     * <p>
     * Header names are matched case-insensitively. If a header name matches one
     * in the sensitive headers list, its value is replaced with
     * "***REDACTED***".</p>
     *
     * @param headers the original headers to mask (can be null)
     * @param sensitiveHeaders the list of header names to mask (can be null or
     * empty)
     * @return new Headers object with sensitive values masked, or empty Headers
     * if input is null
     */
    public static Headers maskHeaders(Headers headers, List<String> sensitiveHeaders) {
        if (headers == null) {
            return new Headers();
        }

        if (sensitiveHeaders == null || sensitiveHeaders.isEmpty()) {
            return headers;
        }

        List<Header> maskedHeaderList = new ArrayList<>();

        for (Header header : headers) {
            boolean isSensitive = false;

            for (String sensitiveHeaderName : sensitiveHeaders) {
                if (header.getName().equalsIgnoreCase(sensitiveHeaderName)) {
                    isSensitive = true;
                    break;
                }
            }

            if (isSensitive) {
                maskedHeaderList.add(new Header(header.getName(), REDACTED_VALUE));
            } else {
                maskedHeaderList.add(header);
            }
        }

        return new Headers(maskedHeaderList);
    }
}
