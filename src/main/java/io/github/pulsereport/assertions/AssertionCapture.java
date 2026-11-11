package io.github.pulsereport.assertions;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isBridge;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

/**
 * Captures assertions — passing and failing — without any change to test code.
 *
 * <p>Instruments the static assertion entry points of JUnit 5
 * ({@code org.junit.jupiter.api.Assertions}), JUnit 4 ({@code org.junit.Assert}),
 * TestNG ({@code org.testng.Assert}, which also backs {@code SoftAssert}),
 * Hamcrest ({@code org.hamcrest.MatcherAssert}) and AssertJ's {@code fail}, plus
 * the checks of AssertJ's fluent assert classes ({@code assertThat(x).isEqualTo(y)})
 * and REST Assured's response checks ({@code then().statusCode(200)}). Each call
 * is recorded into the thread's {@link AssertionCollector}, which adapters open
 * around every step, hook and test.</p>
 *
 * <p>Two ways to enable it:</p>
 * <ul>
 * <li><b>Automatic</b> — adapters call {@link #install()}, which self-attaches a
 * Java agent at runtime. On JDK 21+ the JVM prints a one-time warning about
 * dynamically loaded agents; add {@code -XX:+EnableDynamicAgentLoading} to
 * silence it.</li>
 * <li><b>Explicit</b> — start the JVM with
 * {@code -javaagent:/path/to/pulsereport.jar}. This works on JVMs that forbid
 * dynamic attach and prints no warning.</li>
 * </ul>
 *
 * <p>Disable with the {@code reporter.assertions.capture=false} system property
 * or {@code reporter.properties} entry.</p>
 */
public final class AssertionCapture {

    private static final Logger logger = LoggerFactory.getLogger(AssertionCapture.class);

    static final String[] TARGET_TYPES = {
        AssertionInterpreter.JUNIT5,
        AssertionInterpreter.JUNIT4,
        AssertionInterpreter.TESTNG,
        AssertionInterpreter.HAMCREST,
        AssertionInterpreter.ASSERTJ_FAIL
    };

    /**
     * Static assertion methods. Wrappers that run user code rather than check a
     * value are skipped, which keeps the assertions inside an {@code assertAll}
     * block individually visible.
     */
    private static final ElementMatcher.Junction<MethodDescription> ASSERTION_METHODS = isStatic()
            .and(isPublic())
            .and(nameStartsWith("assert").or(named("fail")).or(named("expectThrows")))
            .and(not(namedOneOf("assertAll", "assertTimeout", "assertTimeoutPreemptively", "assertDoesNotThrow")));

    /**
     * AssertJ checks: the public instance methods of its assert classes that
     * verify something ({@code isEqualTo}, {@code hasSize}, {@code contains}...).
     * Methods that run user code ({@code satisfies}, {@code allSatisfy}...) are
     * skipped, like {@code assertAll}, so the assertions inside them show.
     */
    private static final ElementMatcher.Junction<MethodDescription> ASSERTJ_CHECKS = isPublic()
            .and(not(isStatic()))
            .and(not(isAbstract()))
            .and(not(isBridge()))
            .and(nameStartsWith("is").or(nameStartsWith("has")).or(nameStartsWith("contains"))
                    .or(nameStartsWith("does")).or(nameStartsWith("startsWith")).or(nameStartsWith("endsWith"))
                    .or(nameStartsWith("matches")).or(nameStartsWith("are")).or(nameStartsWith("have"))
                    .or(nameStartsWith("allMatch")).or(nameStartsWith("anyMatch")).or(nameStartsWith("noneMatch"))
                    .or(nameStartsWith("accepts")).or(nameStartsWith("rejects")).or(named("returns")))
            .and(not(nameContains("Satisf")))
            .and(not(named("hashCode")));

    /** REST Assured response checks made after {@code then()}. */
    private static final ElementMatcher.Junction<MethodDescription> REST_ASSURED_CHECKS = isPublic()
            .and(not(isStatic()))
            .and(namedOneOf("statusCode", "statusLine", "contentType", "header", "headers", "cookie", "cookies",
                    "body", "content", "time", "spec"));

    private static volatile boolean installed;
    private static volatile boolean attempted;

    private AssertionCapture() {
    }

    /**
     * Entry point for {@code -javaagent:pulsereport.jar}.
     *
     * @param agentArgs ignored
     * @param instrumentation supplied by the JVM
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        install(instrumentation);
    }

    /**
     * Entry point for dynamic attach.
     *
     * @param agentArgs ignored
     * @param instrumentation supplied by the JVM
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        install(instrumentation);
    }

    /**
     * Installs assertion capture by self-attaching an agent. Idempotent and never
     * throws; a failed attempt is logged once and not retried.
     *
     * @return true if capture is active
     */
    public static synchronized boolean install() {
        if (installed) {
            return true;
        }
        if (attempted) {
            return false;
        }
        attempted = true;
        Instrumentation instrumentation;
        try {
            instrumentation = ByteBuddyAgent.install();
        } catch (Throwable t) {
            logger.warn("PulseReport: assertion capture is unavailable because the agent could not attach ({}). "
                    + "Add -javaagent:<path-to-pulsereport.jar> to the test JVM arguments to enable it.", t.toString());
            return false;
        }
        return install(instrumentation);
    }

    /**
     * @return true if assertion libraries are instrumented in this JVM
     */
    public static boolean isInstalled() {
        return installed;
    }

    static synchronized boolean install(Instrumentation instrumentation) {
        if (installed) {
            return true;
        }
        attempted = true;
        try {
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(new ErrorListener())
                    .type(namedOneOf(TARGET_TYPES), AssertionCapture::seesRecorder)
                    .transform(AssertionCapture::weave)
                    // AssertJ's assert classes; not the proxies it generates for soft assertions and
                    // assumptions, which catch the failure: the check they delegate to records it
                    .type(nameStartsWith(FluentAssertionInterpreter.ASSERTJ_PACKAGE)
                            .and(not(isInterface()))
                            .and(not(nameContains("$ByteBuddy$")))
                            .and(not(nameContains("$$")))
                            .and(hasSuperType(named(FluentAssertionInterpreter.ASSERTJ_ROOT))),
                            AssertionCapture::seesRecorder)
                    .transform((builder, type, loader, module, domain) ->
                            builder.visit(Advice.to(InstanceAssertionAdvice.class).on(ASSERTJ_CHECKS)))
                    .type(named(FluentAssertionInterpreter.REST_ASSURED_CHECKS), AssertionCapture::seesRecorder)
                    .transform((builder, type, loader, module, domain) ->
                            builder.visit(Advice.to(InstanceAssertionAdvice.class).on(REST_ASSURED_CHECKS)))
                    .assureReadEdgeTo(instrumentation, AssertionRecorder.class)
                    .installOn(instrumentation);
            installed = true;
            logger.debug("PulseReport: assertion capture installed");
        } catch (Throwable t) {
            logger.warn("PulseReport: assertion capture could not be installed ({})", t.toString());
        }
        return installed;
    }

    private static DynamicType.Builder<?> weave(DynamicType.Builder<?> builder, TypeDescription type,
                                                ClassLoader loader, JavaModule module,
                                                java.security.ProtectionDomain protectionDomain) {
        return builder.visit(Advice.to(AssertionAdvice.class).on(ASSERTION_METHODS));
    }

    /**
     * The woven bytecode calls {@link AssertionRecorder}; only instrument assertion
     * classes whose class loader resolves that exact class, otherwise the assertion
     * itself would fail with {@code NoClassDefFoundError}.
     */
    private static boolean seesRecorder(ClassLoader loader) {
        if (loader == null) {
            return false;
        }
        try {
            return Class.forName(AssertionRecorder.class.getName(), false, loader) == AssertionRecorder.class;
        } catch (Throwable t) {
            return false;
        }
    }

    private static final class ErrorListener extends AgentBuilder.Listener.Adapter {
        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module,
                            boolean loaded, Throwable throwable) {
            logger.warn("PulseReport: could not instrument {} for assertion capture ({})", typeName, throwable.toString());
        }
    }
}
