package io.github.pulsereport.adapters.restassured;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
     * Matches a Bearer authorization token (JWT-like dotted segments). Group 1
     * captures the "Bearer " prefix so it can be preserved on replacement.
     */
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)(bearer\\s+)[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+\\.?[A-Za-z0-9\\-_=]*");

    /**
     * Matches a JSON Web Token. The {@code eyJ} anchor (base64url of
     * {@code {"}) minimizes false positives on generic dotted strings such as
     * version numbers.
     */
    private static final Pattern JWT_PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]*");

    /**
     * Matches a whole form-urlencoded body ({@code key=value&key=value...}).
     * Possessive quantifiers are used to avoid catastrophic backtracking on
     * large inputs.
     */
    private static final Pattern FORM_URLENCODED_PATTERN = Pattern.compile(
            "^[\\w.\\-]++=[^&]*+(&[\\w.\\-]++=[^&]*+)*+$");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /**
     * Masks sensitive data in an HTTP request/response body.
     *
     * <p>
     * Equivalent to {@link #maskBody(String, List, boolean)} with token
     * masking enabled.</p>
     *
     * @param body the body to mask (can be null)
     * @param sensitiveFields the field names to mask; the list is authoritative
     * and fully replaces any default field list (can be null or empty)
     * @return the masked body, or the original body if null/empty
     */
    public static String maskBody(String body, List<String> sensitiveFields) {
        return maskBody(body, sensitiveFields, true);
    }

    /**
     * Masks sensitive data in an HTTP request/response body.
     *
     * <p>
     * Masking is applied in three ways:</p>
     * <ul>
     * <li><b>JSON bodies</b> (trimmed body starts with {@code {} or
     * {@code [}): parsed with Jackson and traversed recursively. Values whose
     * field name case-insensitively matches a sensitive field are replaced
     * with {@link #REDACTED_VALUE} regardless of value type (text, numbers,
     * booleans and nulls all become the redacted string). Nested objects and
     * arrays are handled at any depth. Note: re-serialization may lose the
     * original formatting (whitespace, key order guarantees).</li>
     * <li><b>XML bodies</b> (trimmed body starts with {@code <}): elements
     * whose tag name matches a sensitive field have their text content
     * replaced; attributes are never masked. See
     * {@link #maskBody(String, List, List, boolean, boolean)} for details.</li>
     * <li><b>Form-urlencoded bodies</b> ({@code key=value&...}): values whose
     * key case-insensitively matches a sensitive field are replaced.</li>
     * <li><b>All body types</b>: if {@code maskTokens} is true, Bearer tokens
     * and JWTs (anchored on the {@code eyJ} prefix to avoid false positives
     * like {@code 1.2.3}) are replaced with {@link #REDACTED_VALUE} via
     * regex.</li>
     * </ul>
     *
     * <p>
     * This method never throws: if structured parsing fails, it falls back to
     * regex-only masking of the raw body. The unmasked body is never
     * logged.</p>
     *
     * @param body the body to mask (can be null)
     * @param sensitiveFields the field names to mask; the list is authoritative
     * and fully replaces any default field list (can be null or empty)
     * @param maskTokens whether to apply the Bearer/JWT regex passes
     * @return the masked body, or the original body if null/empty
     */
    public static String maskBody(String body, List<String> sensitiveFields, boolean maskTokens) {
        return maskBody(body, sensitiveFields, sensitiveFields, true, maskTokens);
    }

    /**
     * Masks sensitive data in an HTTP request/response body, with dedicated
     * control over XML element masking.
     *
     * <p>
     * In addition to the JSON, form-urlencoded and token masking described in
     * {@link #maskBody(String, List, boolean)}, this overload handles
     * <b>XML bodies</b> (trimmed body starts with {@code <}): when
     * {@code maskXmlEnabled} is true, the body is parsed with an XXE-hardened
     * JDK DOM parser and the text content of any element whose tag name (with
     * any namespace prefix stripped) case-insensitively matches an entry in
     * {@code xmlFields} is replaced with {@link #REDACTED_VALUE}. Attributes
     * are never masked. If {@code maskXmlEnabled} is false, XML element
     * masking is skipped but the Bearer/JWT regex pass still applies when
     * {@code maskTokens} is true.</p>
     *
     * <p>
     * This method never throws: if XML parsing or serialization fails, the
     * original body is kept and the regex token pass still applies.</p>
     *
     * @param body the body to mask (can be null)
     * @param sensitiveFields the field names to mask in JSON and
     * form-urlencoded bodies (can be null or empty)
     * @param xmlFields the XML element names to mask (can be null or empty)
     * @param maskXmlEnabled whether XML element masking is applied
     * @param maskTokens whether to apply the Bearer/JWT regex passes
     * @return the masked body, or the original body if null/empty
     */
    public static String maskBody(String body, List<String> sensitiveFields,
            List<String> xmlFields, boolean maskXmlEnabled, boolean maskTokens) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        Set<String> sensitiveFieldSet = toLowerCaseFieldSet(sensitiveFields);
        Set<String> xmlFieldSet = toLowerCaseFieldSet(xmlFields);

        String masked = body;
        String trimmed = body.trim();

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(body);
                maskJsonNode(root, sensitiveFieldSet);
                masked = OBJECT_MAPPER.writeValueAsString(root);
            } catch (Exception e) {
                // Fall back to regex-only masking of the raw body below.
                masked = body;
            }
        } else if (trimmed.startsWith("<")) {
            if (maskXmlEnabled) {
                masked = maskXml(body, xmlFieldSet);
            }
        } else if (FORM_URLENCODED_PATTERN.matcher(trimmed).matches()) {
            masked = maskFormUrlencoded(trimmed, sensitiveFieldSet);
        }

        if (maskTokens) {
            masked = applyTokenMasking(masked);
        }

        return masked;
    }

    /**
     * Builds a lower-cased set from a field name list, ignoring null entries.
     */
    private static Set<String> toLowerCaseFieldSet(List<String> fields) {
        Set<String> fieldSet = new HashSet<>();
        if (fields != null) {
            for (String field : fields) {
                if (field != null) {
                    fieldSet.add(field.toLowerCase(Locale.ROOT));
                }
            }
        }
        return fieldSet;
    }

    /**
     * Masks the text content of sensitive elements in an XML body.
     *
     * <p>
     * The DOM parser is hardened against XXE: DOCTYPE declarations are
     * disallowed and external entities, XIncludes and entity-reference
     * expansion are disabled. Never throws: on any parse/transform failure the
     * original body is returned unchanged so the caller's token-regex pass
     * still applies.</p>
     */
    private static String maskXml(String body, Set<String> sensitiveFieldSet) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE hardening; each feature is set defensively so an unsupported
            // feature never breaks masking.
            setFeatureQuietly(factory,
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureQuietly(factory,
                    "http://xml.org/sax/features/external-general-entities", false);
            setFeatureQuietly(factory,
                    "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            // Prevent error output (e.g. for rejected DOCTYPEs) on stderr.
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
            Document document = builder.parse(new InputSource(new StringReader(body)));

            if (!maskXmlElement(document.getDocumentElement(), sensitiveFieldSet)) {
                // Nothing matched: keep the original body verbatim instead of
                // a re-serialized copy (which would alter the XML declaration
                // and whitespace).
                return body;
            }

            TransformerFactory transformerFactory = harden(TransformerFactory.newInstance());
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return body;
        }
    }

    /**
     * Applies external-access hardening to a transformer factory. Every
     * setting is attempted defensively: not all JDKs/parsers support every
     * attribute, and hardening setup must never fail masking.
     */
    private static TransformerFactory harden(TransformerFactory factory) {
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignored) {
            // Unsupported on this parser.
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (Exception ignored) {
            // Unsupported on this parser.
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception ignored) {
            // Unsupported on this parser.
        }
        return factory;
    }

    /**
     * Sets a parser factory feature, ignoring unsupported features.
     */
    private static void setFeatureQuietly(DocumentBuilderFactory factory,
            String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Parser does not support this feature; never fail masking because
            // of hardening setup.
        }
    }

    /**
     * Recursively replaces the text content of elements whose tag name
     * (namespace prefix stripped) case-insensitively matches a sensitive
     * field. Attributes are never masked.
     *
     * @return true if at least one element was masked
     */
    private static boolean maskXmlElement(Element element, Set<String> sensitiveFieldSet) {
        if (sensitiveFieldSet.contains(localTagName(element).toLowerCase(Locale.ROOT))) {
            element.setTextContent(REDACTED_VALUE);
            return true;
        }
        boolean maskedAny = false;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && maskXmlElement((Element) child, sensitiveFieldSet)) {
                maskedAny = true;
            }
        }
        return maskedAny;
    }

    /**
     * Returns the element tag name without any namespace prefix.
     */
    private static String localTagName(Element element) {
        String name = element.getLocalName();
        if (name == null) {
            name = element.getTagName();
            int colonIndex = name.indexOf(':');
            if (colonIndex >= 0) {
                name = name.substring(colonIndex + 1);
            }
        }
        return name;
    }

    /**
     * Recursively masks textual values of sensitive fields in a JSON tree.
     */
    private static void maskJsonNode(JsonNode node, Set<String> sensitiveFieldSet) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                if (child != null
                        && sensitiveFieldSet.contains(fieldName.toLowerCase(Locale.ROOT))) {
                    // Replace the value regardless of its type: textual,
                    // numeric, boolean and null values all become the
                    // redacted text node.
                    objectNode.put(fieldName, REDACTED_VALUE);
                } else {
                    maskJsonNode(child, sensitiveFieldSet);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskJsonNode(child, sensitiveFieldSet);
            }
        }
    }

    /**
     * Masks values of sensitive keys in a form-urlencoded body.
     */
    private static String maskFormUrlencoded(String body, Set<String> sensitiveFieldSet) {
        String[] pairs = body.split("&");
        StringBuilder result = new StringBuilder(body.length());

        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                result.append('&');
            }
            String pair = pairs[i];
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex > 0
                    && sensitiveFieldSet.contains(
                            pair.substring(0, equalsIndex).toLowerCase(Locale.ROOT))) {
                result.append(pair, 0, equalsIndex + 1).append(REDACTED_VALUE);
            } else {
                result.append(pair);
            }
        }

        return result.toString();
    }

    /**
     * Applies the Bearer and JWT regex masking passes.
     */
    private static String applyTokenMasking(String body) {
        // REDACTED_VALUE contains no '$' or '\', so it is safe as a literal
        // replacement; $1 preserves the original "Bearer " prefix casing.
        String masked = BEARER_PATTERN.matcher(body).replaceAll("$1" + REDACTED_VALUE);
        return JWT_PATTERN.matcher(masked).replaceAll(Matcher.quoteReplacement(REDACTED_VALUE));
    }
}
